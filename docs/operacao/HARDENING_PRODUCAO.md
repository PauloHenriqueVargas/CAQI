# Checklist de Hardening — Antes do Go-Live

Pré-condições para colocar uma instância Sistema CAQ/CAQi em produção.
Cada item DEVE estar marcado antes do deploy. Sem hardening, o sistema
expõe município a riscos legais (LGPD, LRF) e financeiros.

**Não aprovar release que tenha qualquer item marcado como ⚠ pendente.**

---

## 1. Segredos e credenciais

- [ ] **Senhas default trocadas** em todos os serviços
  - `CAQI_LEITOR_PASSWORD`, `CAQI_GESTOR_PASSWORD`, `CAQI_ADMIN_PASSWORD` ≠ `trocar_em_prod`
  - Geradas com `openssl rand -hex 24`
- [ ] **Postgres password** rotacionada
- [ ] **RabbitMQ password** rotacionada
- [ ] **NEXTAUTH_SECRET** gerado com `openssl rand -hex 32`
- [ ] Segredos armazenados em **Vault** ou **External Secrets Operator** — **não** em ConfigMap
- [ ] Logs de aplicação **não** contêm senhas/tokens (busca em `kubectl logs | grep -i password`)
- [ ] CI/CD com workflow secrets — não credenciais em texto claro no repo

## 2. Identidade e RBAC

- [ ] **Gov.br OAuth2** habilitado *(Fase 9.B)* — Basic Auth fica só para acesso técnico de emergência
- [ ] **MFA TOTP** ativo para usuários ADMIN *(Fase 9.B)*
- [ ] Usuários do sistema mapeados — leitor/gestor/admin atribuídos a pessoas reais (não compartilhados)
- [ ] Política de senha forte (≥ 16 chars) ou autenticação federada
- [ ] Procedimento de revogação de acesso documentado e testado
- [ ] RBAC validado: tentar POST como leitor → 403; PUT como gestor → 403

## 3. Rede

- [ ] **TLS 1.3** obrigatório em todos os endpoints públicos
- [ ] Certificados via **cert-manager** + Let's Encrypt; renovação automática verificada
- [ ] **NetworkPolicy** ativa (`networkPolicy.enabled: true`)
- [ ] Postgres **NÃO** acessível fora do namespace
- [ ] RabbitMQ management UI **NÃO** exposta publicamente
- [ ] Ingress com header `Strict-Transport-Security: max-age=31536000`
- [ ] WAF / proteção DDoS no Ingress (Cloudflare / AWS Shield) — opcional para piloto

## 4. Banco de dados

- [ ] **Encryption at rest** habilitada (RDS gp3 + KMS, ou TDE em postgres self-hosted)
- [ ] `pg_hba.conf` aceita apenas conexões do namespace via TLS (não `trust`)
- [ ] Usuário `caqi` da aplicação **sem** privilégio de SUPERUSER ou CREATEDB
- [ ] Usuário separado para migração (Flyway) com privilégios elevados, usado **somente** durante deploy
- [ ] **Backup** ativo (`backup.enabled: true`) e drill mensal executado
- [ ] **Point-in-time recovery** habilitado (recomendado)
- [ ] Logs do Postgres rotacionando, sem texto claro de queries com PII

## 5. Auditoria e compliance

- [ ] **Cadeia SHA-256** verificada na primeira inicialização — `GET /compliance/auditoria/verificar` → `{integro: true}`
- [ ] Tabela `log_auditoria` **NÃO** tem privilégio de UPDATE para usuário `caqi` (só INSERT/SELECT)
- [ ] Tabela `log_auditoria` com **trigger de proteção** contra UPDATE/DELETE *(opcional, defesa em profundidade)*
- [ ] **DPIA/RIPD** preenchido e aprovado pelo DPO (`docs/legal/DPIA_RIPD.md`)
- [ ] **ROPA** atualizado e revisado (`docs/legal/ROPA.md`)
- [ ] **Política de privacidade** publicada no portal de transparência (`docs/legal/POLITICA_PRIVACIDADE.md`)
- [ ] Encarregado (DPO) designado e contato divulgado
- [ ] Procedimento de comunicação de incidente LGPD definido (24h ANPD)

## 6. Observabilidade

- [ ] **OpenTelemetry collector** ativo — traces fluindo para Tempo/Jaeger
- [ ] **Prometheus + Grafana** configurados; dashboards básicos importados (JVM, Postgres, RabbitMQ, RED metrics)
- [ ] **Alertas** configurados:
  - 5xx rate > 1% em 5min
  - Pool de conexões > 80%
  - Espaço em disco < 20%
  - Listener consumer lag > 100 mensagens
  - Cadeia SHA-256 quebrada (job que roda /verificar a cada hora)
- [ ] **Logs estruturados** (JSON) indexados em Loki ou similar
- [ ] **Retenção de logs** ≥ 90 dias (TCE pode exigir 5 anos para registros financeiros)

## 7. Compliance enforcers

- [ ] Decisão sobre `caqi.compliance.bloquear-empenhos-violadores`:
  - **Recomendado em prod:** `false` (apenas notifica) até que setor financeiro esteja maduro
  - Quando ligar: comunicar gestores, ter runbook de exceção pronto
- [ ] `caqi.events.enabled = true`
- [ ] Listener compliance recebe eventos — testado com cálculo real

## 8. Deploy

- [ ] Imagens **com tag explícita** (não `latest`)
- [ ] Imagens assinadas (cosign / Sigstore) — **opcional** mas recomendado
- [ ] `imagePullPolicy: IfNotPresent` (evita pull desnecessário em cada restart)
- [ ] **PodSecurityContext** restritivo (`runAsNonRoot: true`, `readOnlyRootFilesystem: true`)
- [ ] **Resource requests/limits** configurados para todos os contêineres
- [ ] **HPA** ou **VPA** configurado para os serviços críticos
- [ ] **PodDisruptionBudget** com `minAvailable: 1` para serviços com 2+ replicas

## 9. Disaster recovery

- [ ] Backup testado (drill ≥ 1 vez)
- [ ] Tempo de RTO medido e documentado
- [ ] Procedimento de failover para outra região (se aplicável)
- [ ] Acesso de emergência ao banco documentado e testado (break-glass account)
- [ ] Inventário de dependências externas (FNDE, INEP, PNCP, gov.br) — fallback se indisponíveis

## 10. Documentação

- [ ] **Runbook** específico do município preenchido (`docs/operacao/RUNBOOK.md` Apêndice A)
- [ ] Versão atual do sistema documentada (commit hash + tag)
- [ ] Inventário de usuários ADMIN com data de criação
- [ ] Treinamento ministrado para usuários (gestor, admin)
- [ ] Manual do usuário publicado

## 11. Pen-test

- [ ] **Pen-test interno** realizado (mínimo: OWASP Top 10, IDOR nas APIs autenticadas)
- [ ] Vulnerabilidades altas/críticas remediadas antes do go-live
- [ ] Pen-test externo (terceiro independente) — recomendado anualmente

## 12. Validação funcional final

- [ ] Smoke test E2E rodou green (`scripts/smoke-test.sh`)
- [ ] Cálculo CAQ contra dados reais do município bate com a planilha base (±0,5%)
- [ ] Avaliador compliance gera notificações esperadas para o cenário do município
- [ ] Geração SIOPE: revisão pelo setor de contabilidade aprovou o quadro

---

## Aprovações finais

Documentar em ata anexa:

- [ ] **Secretário(a) de Educação** — aprovação operacional
- [ ] **Secretário(a) de Finanças** — aprovação fiscal
- [ ] **Procurador(a)** — análise jurídica + LGPD
- [ ] **Encarregado(a) (DPO)** — compliance LGPD
- [ ] **Controle interno** — ciência

Após go-live: revisar este checklist a cada 6 meses ou em mudança material.
