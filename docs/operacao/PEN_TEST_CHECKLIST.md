# Checklist de Pen-Test Interno

Roteiro estruturado para avaliação periódica de segurança do Sistema
CAQ/CAQi seguindo **OWASP Top 10 (2021)** + controles específicos do
domínio (LGPD, LRF, Lei 14.133).

## Cadência

| Tipo | Frequência | Responsável |
|---|---|---|
| Scan automatizado (`scripts/security-scan.sh`) | a cada PR + diário em main | CI |
| Pen-test interno completo (este checklist) | quadrimestral | DPO + dev sênior |
| Pen-test externo (consultoria) | anual | empresa contratada com NDA |
| Re-test pós-correção de achado HIGH/CRITICAL | imediato após patch | mesmo time do achado |

## Precondições

1. Ambiente de testes separado de produção (réplica do cluster com dados sintéticos).
2. **Autorização formal por escrito** do controlador (município) — pen-test sem
   autorização configura crime (Lei 12.737/2012).
3. Backup recente do ambiente de testes — testes destrutivos podem corromper estado.
4. Conta dedicada de pen-tester com role separada (não usar `admin_test`) e
   senha registrada no cofre do DPO.

## OWASP Top 10 (2021) — casos específicos do CAQi

### A01:2021 — Broken Access Control

- [ ] **IDOR em `/api/v1/caqi/calculos/{id}`** — usuário LEITOR de Município A
      tenta acessar `calculo_id` de Município B. Esperado: 404 (em prod
      com 1 instância por município o risco é baixo, mas validar).
- [ ] **Bypass de RBAC POST/PUT** — LEITOR tenta criar despesa
      (`POST /financeiro/despesas`) → esperar **403 Forbidden**.
      Validar para todos os endpoints com matriz: LEITOR×GET, GESTOR×POST,
      ADMIN×PUT/DELETE.
- [ ] **Path traversal no upload Censo** —
      `POST /api/v1/escolar/censo/import` com `arquivo=../../etc/passwd`.
      Esperado: rejeição (Spring Boot multipart já sanitiza).
- [ ] **Bypass do bloqueador de empenho** — POST despesa violadora com
      `?dryRun=true` ou header customizado tentando burlar a flag.
      Esperado: 409 Conflict mantido.
- [ ] **Endpoint público sem auth** — `/api/public/transparencia/**`
      acessível anonimamente (esperado), mas verificar que **não vaza
      dados pessoais** (CPF, nome aluno, payload sensível).

### A02:2021 — Cryptographic Failures

- [ ] **TLS** — `nmap --script ssl-enum-ciphers -p 443 <host>` deve listar
      apenas TLS 1.2+ com cipher suites strong (ECDHE+AES-GCM).
- [ ] **HSTS** — header `Strict-Transport-Security: max-age=31536000; includeSubDomains`
      em todas respostas HTTPS.
- [ ] **Cookies de sessão** — `next-auth.session-token` com `Secure`,
      `HttpOnly`, `SameSite=Lax`.
- [ ] **CPF e dados sensíveis** — verificar que `pessoa_servidor.cpf_hash`
      é SHA-256 (sem reverso possível) e que `usuario_mfa.secret_base32`
      não vai para logs (`-XGET /actuator/loggers` não deve mostrar).
- [ ] **Salt de pseudonimização Censo** —
      `caqi.censo.pseudonimizacao-salt` deve ser secret K8s (não env var
      em ConfigMap), com pelo menos 32 bytes aleatórios em prod.

### A03:2021 — Injection

- [ ] **SQL injection** — testar `?ano=2025'; DROP TABLE escola; --`
      em todos os parâmetros query. Esperado: rejeição (Spring Data JPA
      usa prepared statements).
- [ ] **Command injection** no upload de Censo — nome de arquivo com
      `; rm -rf /`. Esperado: nome sanitizado pelo MultipartFile.
- [ ] **Log injection** — usuário com nome `admin\nERROR Backdoor enabled`
      → verificar que log não interpreta como nova linha (Logback default OK).
- [ ] **JPQL injection** — `@Query` com parâmetros: já usam `:param`
      bindings (verificar via grep `String.format` em `*Repository.java`).

### A04:2021 — Insecure Design

- [ ] **Rate limiting no /login** — 1000 requests/seg → esperar 429 após
      threshold. Hoje: NextAuth não tem rate limit nativo; mitigar via
      ingress nginx `limit_req_zone`.
- [ ] **Enumeração de usuários** — login com user inexistente vs senha
      errada deve retornar **mensagem idêntica** (ver login form). ✓
- [ ] **Enumeração de MFA** — `/api/v1/auth/mfa/status` revela se usuário
      tem MFA. Esperado: endpoint exige autenticação. ✓ (já é authenticated())

### A05:2021 — Security Misconfiguration

- [ ] **Headers de segurança** — verificar via `curl -I`:
  - `X-Frame-Options: DENY` (anti-clickjacking)
  - `X-Content-Type-Options: nosniff`
  - `Content-Security-Policy` com `default-src 'self'`
  - `Referrer-Policy: strict-origin-when-cross-origin`
- [ ] **Atuator exposto** — `/actuator/env`, `/actuator/heapdump`,
      `/actuator/loggers` devem dar **403** (apenas `health`, `info`,
      `metrics`, `prometheus` em SecurityConfig).
- [ ] **Swagger UI em prod** — Springdoc UI exposta? Decisão: deixar
      acessível publicamente (`permitAll`) **apenas se não vazar
      endpoints sensíveis**. Hoje: visível mas não revela dados.
- [ ] **Default credentials** — `admin` / `trocar_em_prod` rejeitado em
      prod (env var obrigatório no Helm).

### A06:2021 — Vulnerable Components

- [ ] **Imagens base** scan — `trivy image ghcr.io/caqi/caq-engine-svc:VERSION`,
      `--severity HIGH,CRITICAL --exit-code 1` (já no `security-scan.sh`).
- [ ] **Java deps** — `./gradlew dependencyCheckAggregate` (OWASP
      Dependency-Check); falhar build em CVSS ≥ 7.0.
- [ ] **npm deps** — `npm audit --audit-level=high` no `apps/web`.
- [ ] **dbt packages** — versão pinada em `analytics/packages.yml`.

### A07:2021 — Identification and Authentication Failures

- [ ] **Senhas fracas** — política mínima documentada: 12 chars, mix
      maiúscula+minúscula+dígito+símbolo. Hoje: SecurityConfig usa
      `BCrypt`; força a aplicar na origem (Gov.br OAuth2 cumprirá em prod).
- [ ] **Tentativas de login** — sem lockout após N falhas. Mitigação:
      ingress rate limit + monitoring em log de auditoria.
- [ ] **MFA TOTP** — código reusado dentro da janela de 30s deve falhar
      (MfaService usa `setAllowedTimeWindow(1)` permitindo ±30s; uma
      tentativa por código). Backup codes one-time-use validar.
- [ ] **JWT NextAuth** — assinatura com `NEXTAUTH_SECRET` ≥ 32 bytes
      aleatórios. Decode JWT manualmente: payload deve ter `exp` (8h
      TTL).

### A08:2021 — Software and Data Integrity Failures

- [ ] **Cadeia de auditoria SHA-256** — V0006 trigger bloqueia UPDATE/
      DELETE em `log_auditoria`. Tentar `UPDATE log_auditoria SET acao='hack'
      WHERE log_id=1;` → deve falhar com erro 42501.
- [ ] **Hash de publicação LRF 48-A** — recalcular SHA-256 do
      `publicacao_portal.snapshot` e comparar com `conteudo_hash`. Zero
      divergências esperadas. (Endpoint `/verificar` no defer da 6.C.)
- [ ] **Artefatos de build** — imagens publicadas com Docker Content Trust
      ou cosign (defer). Verificar checksum SHA-256 das imagens em prod
      contra registry.

### A09:2021 — Security Logging and Monitoring Failures

- [ ] **Eventos críticos logados**: tentativa login falhada, bloqueio
      empenho (409), violação Fundeb (notificação criada), import Censo
      (com hash). Verificar em `/api/v1/compliance/auditoria`.
- [ ] **Sem PII em logs** — `grep -E "[0-9]{11}|cpf=" /var/log/caqi/*.log`
      → 0 resultados (CPF nunca em log).
- [ ] **Alerta para chain quebrada** —
      `/api/v1/compliance/auditoria/verificar` rodando em CronJob; se
      retornar `Optional<Long>` não-vazio, dispara alerta crítico para
      DPO + DevOps.
- [ ] **Retenção mínima 1 ano** — Loki/CloudWatch configurado com
      retention 365 dias para logs de auditoria.

### A10:2021 — Server-Side Request Forgery

- [ ] **Upload Censo HTTP cliente futuro (Sprint 5.B defer)** — quando
      o download automático do ZIP INEP entrar, validar que URL é
      whitelisted (`download.inep.gov.br` apenas).
- [ ] **PNCP integration (Sprint 4.C defer)** — mesmo para o cliente
      PNCP: only allow `pncp.gov.br/*`.
- [ ] **OAuth2 Gov.br (Sprint 9.B defer)** — only redirect para
      `sso.acesso.gov.br/*`; rejeitar `redirect_uri` arbitrário.

## Controles específicos do domínio

### LGPD

- [ ] **Direitos do titular** (LGPD art. 18) — interfaces existem em
      `docs/legal/POLITICA_PRIVACIDADE.md`? Endpoint `GET /api/v1/lgpd/dados`
      está implementado? **Defer** explicito até implementação.
- [ ] **Vazamento de pseudonimização** — tentar reverso do
      `id_aluno_hash` por brute force com salt vazado. Esperar:
      computacionalmente inviável (SHA-256 + salt ≥8 bytes).
- [ ] **Comunicação de incidente** — playbook em RUNBOOK.md §9 inclui
      contato ANPD em até 48h?

### LRF / LAI

- [ ] **48-A (24h)** — verificar última `data_publicacao` em
      `publicacao_portal` < 24h após última despesa registrada.
- [ ] **Conteúdo público** consistente com endpoint público
      (`/api/public/transparencia/**`) e snapshot armazenado.

### Lei 14.133/2021

- [ ] **Segregação de função** — usuário GESTOR consegue criar contrato
      (POST) **e também** medição (POST) **e também** despesa (POST)?
      Hoje: sim — em prod recomenda-se **users distintos** por função
      (defer: roles `CONTRATADOR` e `LIQUIDANTE`).

## Ferramentas recomendadas

| Camada | Ferramenta | Comando |
|---|---|---|
| SAST código | Semgrep | `semgrep --config=p/owasp-top-ten apps/` |
| Image scan | Trivy | `trivy image ghcr.io/caqi/...` |
| DAST passivo | OWASP ZAP baseline | `zap-baseline.py -t http://localhost:3000` |
| Secrets em código | gitleaks | `gitleaks detect --no-banner` |
| Deps Java | OWASP Dependency-Check | `./gradlew dependencyCheckAggregate` |
| Deps npm | `npm audit` | `npm audit --audit-level=high` |
| TLS | nmap + sslyze | `sslyze --regular <host>:443` |
| K8s manifests | kubescape | `kubescape scan charts/caqi/` |

Wrapper: `scripts/security-scan.sh` executa o conjunto mínimo localmente
(equivalente CI) com saída agregada.

## Reportagem

Cada execução do checklist produz:

1. **Arquivo `pen-test/YYYY-MM-DD-NN.md`** com:
   - Escopo (versão do app, ambiente, limites éticos)
   - Achados (id, severidade CVSS 3.1, descrição, prova de conceito,
     recomendação)
   - Status pós-correção
2. **Comunicação ao DPO** se houver achado >= MEDIUM.
3. **Ata do CACS-Fundeb / CME** com resumo executivo trimestralmente.
4. **Atualização da DPIA/RIPD** se o achado afetar tratamento de dados
   pessoais.
