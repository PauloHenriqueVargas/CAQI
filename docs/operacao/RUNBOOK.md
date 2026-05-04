# Runbook Operacional — Sistema CAQ/CAQi

Procedimentos para incidentes comuns. Quem está oncall consulta primeiro
**aqui**; se o sintoma não estiver listado, escala para o time de plataforma.

**Convenções:** comandos `kubectl` assumem `-n caqi-municipio-<id>`. Para
docker-compose local, troque por `docker compose`.

---

## 0. Triagem rápida (5 min)

```bash
# 1. Health geral dos pods
kubectl get pods -l app.kubernetes.io/instance=caqi-<municipio>

# 2. Eventos recentes (últimos 30 min)
kubectl get events --sort-by=.lastTimestamp | tail -30

# 3. Liveness dos serviços
for svc in engine financeiro escolar compliance web; do
  echo "=== $svc ==="
  kubectl exec deploy/caqi-<municipio>-$svc -- curl -fsSL http://localhost:$port/actuator/health || echo "DOWN"
done

# 4. Status do banco
kubectl exec -it $(kubectl get pod -l role=postgres -o name) -- psql -U caqi -c "SELECT now(), version();"
```

---

## 1. Sintoma: 503 / 5xx em rajada

### Diagnóstico
```bash
# Erros recentes em todos os serviços
kubectl logs -l app.kubernetes.io/instance=caqi-<municipio> --since=10m --tail=200 | grep -iE "ERROR|WARN" | sort -u | head -50

# Pods em CrashLoopBackOff?
kubectl get pods | grep -E "CrashLoopBackOff|Error|ImagePullBackOff"

# Recursos esgotados?
kubectl top pods --containers | sort -k4 -h | tail -10
```

### Causas comuns
| Sintoma | Provável causa | Mitigação imediata |
|---|---|---|
| `OutOfMemoryError` em logs Spring | Heap insuficiente em pod | Aumentar `resources.limits.memory` no values.yaml; reiniciar pod |
| `connection refused` ao postgres | DB caiu ou network policy mudou | Ver §3 |
| `No connection available` no Hikari | Pool esgotado | Aumentar `maximum-pool-size` em application.yml; verificar queries lentas |
| `AmqpConnectException` | RabbitMQ caiu | Ver §4 |
| 401 em massa | SecurityConfig regrediu ou Vault perdeu segredos | Rollback do release Helm |

---

## 2. Sintoma: deploy ficou travado

### Diagnóstico
```bash
helm status caqi-<municipio>                              # Release status
kubectl rollout status deploy/caqi-<municipio>-engine      # Rollout
kubectl describe deploy/caqi-<municipio>-engine | tail -30 # Eventos
kubectl get rs -l app.kubernetes.io/instance=...           # ReplicaSets
```

### Mitigação
```bash
# Rollback se for o caso
helm rollback caqi-<municipio> <revisao-anterior>

# Forçar re-pull (se for ImagePullBackOff)
kubectl rollout restart deploy/caqi-<municipio>-engine
```

**Não force `helm upgrade --force`** — pode mascarar o problema. Identifique
a causa primeiro.

---

## 3. Sintoma: PostgreSQL indisponível

### Diagnóstico
```bash
# Pod do postgres
kubectl get pod -l role=postgres
kubectl logs -l role=postgres --tail=100

# Espaço em disco do PVC
kubectl exec $(kubectl get pod -l role=postgres -o name) -- df -h /var/lib/postgresql/data

# Conexões ativas
kubectl exec -it $(kubectl get pod -l role=postgres -o name) -- \
  psql -U caqi -c "SELECT count(*) FROM pg_stat_activity;"

# Locks longos
kubectl exec -it $(kubectl get pod -l role=postgres -o name) -- \
  psql -U caqi -c "SELECT pid, now()-xact_start AS dur, query FROM pg_stat_activity WHERE state='active' ORDER BY dur DESC LIMIT 10;"
```

### Mitigação
| Causa | Ação |
|---|---|
| Disco cheio | Aumentar PVC; rotacionar logs; vacuum full em tabelas inchadas |
| Conexões esgotadas | Reduzir `maximum-pool-size` dos clientes; matar idle connections com `pg_terminate_backend()` |
| Replication lag (se houver réplica) | Verificar rede; pausar writes pesados |
| Postgres crashed | Restaurar do backup mais recente — ver `BACKUP_RESTORE.md` |

---

## 4. Sintoma: RabbitMQ congestionado / listener parou

### Diagnóstico
```bash
# Painel RabbitMQ
kubectl port-forward svc/caqi-rabbitmq 15672:15672
# abrir http://localhost:15672 — login: caqi/<senha>

# Filas com backlog
kubectl exec -it caqi-rabbitmq-0 -- rabbitmqctl list_queues name messages messages_ready messages_unacknowledged

# Listener do compliance-svc
kubectl logs deploy/caqi-<municipio>-compliance | grep -i listener
```

### Mitigação
| Sintoma | Ação |
|---|---|
| Fila `caqi.compliance.calculo-executado.<id>` empilhando | Listener morreu — reiniciar pod compliance-svc |
| `unroutable` aumentando | Routing key não casa — verificar binding `caqi.calculo.executado.<id>` no consumer |
| Consumer ack lento | Aumentar `concurrency` no listener; verificar AvaliadorComplianceService chamando financeiro lento |
| Memória RabbitMQ alta | Mover queues persistentes; verificar limit de mensagens |

**Idempotência**: o listener atual é idempotente (registra em log_auditoria com chain hash — duplicata gera dois logs distintos mas a cadeia continua íntegra). Republish de DLQ é seguro.

---

## 5. Sintoma: Validador Fundeb bloqueou empenho não esperado

Aplica-se quando `caqi.compliance.bloquear-empenhos-violadores=true`.

### Diagnóstico
```bash
# Última mensagem de bloqueio
kubectl logs deploy/caqi-<municipio>-financeiro --since=1h | grep "EmpenhoBloqueado"
```

A mensagem traz a vinculação que caiu (MDE/Fundeb70/VAAT15) e os percentuais
**antes** vs **depois** da despesa simulada.

### Decisões possíveis
1. **Despesa estava mal classificada** — corrigir natureza/fonte/siope_grupo e tentar novamente
2. **Município já estava em violação antes** — nesse caso o bloqueador NÃO age (só age na transição cumpre→não-cumpre); confirme via `GET /api/v1/fundeb/execucao?ano=N`
3. **Bloqueador agindo corretamente** — o gestor precisa ajustar plano (compensar com despesa de pessoal Fundeb antes de lançar a violadora)
4. **Emergência operacional** — desligar a flag temporariamente:
   ```bash
   kubectl set env deploy/caqi-<municipio>-financeiro CAQI_BLOQUEAR_EMPENHOS_VIOLADORES=false
   # NÃO esquecer de religar e RIPD do incidente
   ```

---

## 6. Sintoma: Cadeia de auditoria reportou quebra

### Diagnóstico
```bash
curl -u admin:<senha> http://localhost:8084/api/v1/compliance/auditoria/verificar
# {"integro": false, "primeiroLogQuebrado": 1234}
```

### Investigação
1. **Não tente "consertar" o log_auditoria.** A integridade quebrada é informação valiosa para auditoria.
2. Backup do banco deve incluir `log_auditoria` — restaurar de snapshot anterior à adulteração para identificar quem modificou.
3. Comparar hash_depois esperado vs persistido no log id quebrado:
   ```sql
   SELECT log_id, tabela, registro_id, acao, carimbo_tempo,
          hash_antes, hash_depois,
          encode(digest(hash_antes||'|'||tabela||'|'||registro_id||'|'||acao||'|'||carimbo_tempo, 'sha256'), 'hex') AS recalculado
   FROM log_auditoria WHERE log_id = 1234;
   ```
4. Notificar DPO + controle interno + TCE conforme protocolo de incidente.

**Causa-raiz comum:** restauração parcial do banco (alguém restaurou só certas tabelas e a cadeia ficou descontínua). Outras causas implicam tentativa de adulteração — escalar.

---

## 7. Sintoma: SIOPE export retorna pendências em massa

```bash
curl -u leitor:<senha> 'http://localhost:8082/api/v1/siope/status?ano=2025'
```

Tipos comuns:
- **Receita sem `origem`** → setor financeiro precisa classificar
- **Despesa sem `siope_grupo`** → idem
- **Despesa de capital com fonte ≠ VAAT** → reclassificação ou justificativa

Ação: gerar planilha de pendências, encaminhar para o setor responsável,
estabelecer prazo. **Não publicar SIOPE** com pendências críticas — fica
exposto em fiscalização TCE.

---

## 8. Sintoma: TLS / certificado expirado

cert-manager faz a renovação automática (Let's Encrypt). Falhas em renovar:

```bash
kubectl get certificate -A
kubectl describe certificate caqi-tls -n caqi-<municipio>
kubectl logs -n cert-manager deploy/cert-manager
```

Causas comuns: DNS não resolvendo (challenge HTTP-01), rate limit do Let's
Encrypt (5 falhas → 1h block), Issuer ClusterIssuer indisponível.

---

## 9. Comunicação de incidente LGPD

Se houver vazamento confirmado ou suspeito de dados pessoais:

1. **Em até 1h**: notificar DPO + Secretário + Procurador
2. **Em até 24h**: ANPD (LGPD art. 48) — formulário público
3. **Em até 72h**: titulares afetados (quando exigir o caso)
4. Documentar em `docs/legal/incidentes/<data>-<resumo>.md`

---

## Apêndice A — Contatos

(Preencher por município no fork do runbook.)

| Papel | Nome | Telefone | Email |
|---|---|---|---|
| Oncall técnico | | | |
| DPO | | | |
| Secretário Educação | | | |
| Suporte Controller | | | |
