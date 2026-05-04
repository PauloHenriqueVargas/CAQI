# Backup e Restauração

Estratégia de backup do PostgreSQL para atender LGPD (disponibilidade) +
LRF (preservação de evidências por 5 anos) + boas práticas TCE.

---

## Estratégia

### Camadas

1. **pg_dump diário** (lógico)
   - 02:00 UTC, todos os dias
   - Comprimido com `-Fc` (custom format), encriptado com GPG ou cifragem do bucket
   - Retenção: 30 diários + 12 mensais (1°) + 7 anuais (1° de janeiro)
   - Local: bucket S3 ou MinIO com encryption-at-rest via KMS

2. **WAL contínuo (PITR)** *— recomendado para prod, opcional para piloto*
   - WAL archive a cada 60s
   - Permite restaurar até qualquer ponto no tempo (RPO ≤ 60s)
   - Requer Postgres com `archive_mode = on`

3. **Snapshot do volume** *— se RDS/CloudSQL gerenciado*
   - Snapshot diário gerenciado pelo provedor
   - Cross-region replica (DR)

### RPO / RTO alvo

| Cenário | RPO | RTO |
|---|---|---|
| Piloto (apenas pg_dump diário) | 24h | 4h |
| Prod com PITR | 60s | 1h |
| DR cross-region | 5 min replica lag | 4h failover manual |

---

## CronJob Helm

O chart `caqi` inclui `templates/cronjob-backup.yaml`. Habilitar via
`values.yaml`:

```yaml
backup:
  enabled: true
  schedule: "0 2 * * *"            # diário 02:00 UTC
  retention:
    diario: 30
    mensal: 12
    anual:  7
  destination:
    type: s3                       # s3 | minio
    bucket: caqi-municipio-exemplo-backups
    region: sa-east-1
    kmsKeyId: arn:aws:kms:sa-east-1:000:key/...
  resources:
    requests: { cpu: 100m, memory: 256Mi }
    limits:   { cpu: 500m, memory: 512Mi }
```

O CronJob roda `pg_dump` em container `postgres:16-alpine`, comprime, envia
para S3 com `aws s3 cp --sse aws:kms --sse-kms-key-id <kmsKeyId>`. A
identidade IAM do pod (IRSA no EKS) precisa ter `s3:PutObject` no bucket.

---

## Restauração — passo a passo

### Pré-requisitos
- Acesso ao bucket de backup (credencial AWS ou MinIO)
- Postgres alvo já criado e vazio (ou faz drop-recreate do schema)
- Aplicação **parada** durante a restauração

### Procedimento

```bash
# 1. Para a aplicação para evitar writes durante restore
helm upgrade caqi-<municipio> ./charts/caqi \
  --set services.engine.replicas=0 \
  --set services.financeiro.replicas=0 \
  --set services.escolar.replicas=0 \
  --set services.compliance.replicas=0 \
  --set services.web.replicas=0 \
  --reuse-values

# 2. Identifica o backup desejado
aws s3 ls s3://caqi-municipio-exemplo-backups/ --recursive | sort -r | head

# 3. Baixa
aws s3 cp s3://caqi-.../caqi-2026-05-04T02-00.dump.gpg ./

# 4. Decripta (se aplicável)
gpg --decrypt caqi-2026-05-04T02-00.dump.gpg > caqi.dump

# 5. Drop schema atual e recria
kubectl exec -it $(kubectl get pod -l role=postgres -o name) -- \
  psql -U caqi -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# 6. Restaura com pg_restore
kubectl exec -i $(kubectl get pod -l role=postgres -o name) -- \
  pg_restore -U caqi -d caqi --no-owner --no-acl --verbose < caqi.dump

# 7. Verifica integridade
kubectl exec -it $(kubectl get pod -l role=postgres -o name) -- \
  psql -U caqi -c "SELECT count(*) FROM calculo_caq; SELECT count(*) FROM log_auditoria;"

# 8. Verifica cadeia SHA-256 da auditoria
curl -u admin:<senha> http://localhost:8084/api/v1/compliance/auditoria/verificar
# Esperado: {"integro": true}

# 9. Religa a aplicação
helm upgrade caqi-<municipio> ./charts/caqi --reuse-values \
  --set services.engine.replicas=1 ...
```

### Restauração para PITR (opcional, se WAL archive ativo)

```bash
# Restaura base + WAL até timestamp específico
pg_basebackup -h <postgres> -D /var/lib/postgresql/data
recovery_target_time = '2026-05-04 14:30:00 BRT'  # em recovery.signal
```

Consultar [docs Postgres PITR](https://www.postgresql.org/docs/16/continuous-archiving.html) na hora.

---

## Drill mensal obrigatório

**Toda primeira segunda-feira do mês**, time de plataforma:
1. Sobe ambiente de staging com restauração do backup mais recente
2. Roda smoke test (`scripts/smoke-test.sh`) + verificarIntegridade
3. Mede tempo total de restauração
4. Documenta resultado em `docs/operacao/drills/YYYY-MM.md`
5. Se RTO > meta, abre PR de melhoria (script de restauração mais rápido,
   shards menores, etc.)

**Backup que nunca foi testado é igual a NENHUM backup.** Disciplina.

---

## Considerações LGPD

- Backups contêm PII e seguem o mesmo prazo de retenção dos dados originais
- Bucket de backup com bloqueio de exclusão (`s3:PutObjectRetention` / Object Lock)
- Acesso ao bucket auditado (CloudTrail / equivalente)
- Cross-region replication para outra região BR (não internacional sem base legal específica)
- Após o prazo de retenção, **eliminação criptográfica** (rotação da KMS Key) é aceitável
