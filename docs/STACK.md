# Stack Tecnológica

Decisões aplicadas ao repositório. Referência permanente — qualquer mudança gera ADR no `docs/adr/` (futuro).

## Princípios

1. **Microserviços já no MVP** — 4 serviços de domínio + 1 Next.js BFF/UI. Fronteiras claras; possível extração de serviços adicionais conforme necessidade.
2. **Schema-per-service em DB compartilhado (MVP)** → database-per-service em Fase 2. Mantém complexidade operacional sob controle no início.
3. **Instância isolada por município** (não multi-tenant compartilhado) — 1 release Helm por município, namespace k8s dedicado.
4. **Cloud-agnóstico via Kubernetes + Helm** — funciona em AWS/Azure/GCP/OCI BR e on-prem.
5. **API-first** com OpenAPI 3.1 gerado pelos próprios serviços (springdoc-openapi).
6. **Conformidade auditável por construção** — regras legais (MDE 25%, Fundeb 70%/15%, VAAR) no domínio, não na UI.
7. **Proprietária, todos os direitos reservados.**

## Backend — microserviços Spring Boot

| Item | Escolha |
|---|---|
| Linguagem | **Java 21** (LTS, virtual threads habilitadas) |
| Build | **Gradle 8.10 + Kotlin DSL + version catalog** (`gradle/libs.versions.toml`) |
| Framework | **Spring Boot 3.3.x** + **Spring Cloud 2023.0.x** |
| ORM/Migrations | **Spring Data JPA + Hibernate**; **Flyway** (apenas no `caq-engine-svc` — owner do schema) |
| Driver | **postgresql 42.7** |
| Validação | **Bean Validation (Jakarta)** |
| Mensageria | **RabbitMQ** via `spring-boot-starter-amqp` |
| Resiliência | **Resilience4j** (`spring-cloud-starter-circuitbreaker-resilience4j`) |
| OpenAPI | **springdoc-openapi 2.6** — Swagger UI em `/swagger-ui.html` |
| Mapping | **MapStruct 1.5** |
| Boilerplate | **Lombok 1.18** |
| Tests | **JUnit 5 + Spring Boot Test + Testcontainers** (Postgres + RabbitMQ reais) |

### Microserviços

| Serviço | Porta | Responsabilidade | Schema |
|---|---|---|---|
| `caq-engine-svc` | 8081 | Motor CAQ/CAQi, parâmetros, insumos, índices, simulações; **owner Flyway** | `engine` |
| `caq-financeiro-svc` | 8082 | Orçamento, Fundeb, MDE, contabilidade SIAFIC/PCASP, tributário | `financeiro` |
| `caq-escolar-svc` | 8083 | Escolas, turmas, matrículas, censo, PNAE/PNATE, pessoal/folha | `escolar` |
| `caq-compliance-svc` | 8084 | Auditoria imutável, validadores legais, ROPA/LGPD, LAI, SIOPE | `compliance` |

Comunicação inter-serviços: **REST síncrono** para queries cross-domain; **eventos RabbitMQ** para fan-out (ex.: matrícula criada → notifica financeiro/compliance/engine).

## Frontend — Next.js + BFF

| Item | Escolha |
|---|---|
| Framework | **Next.js 14 (App Router) + TypeScript** |
| UI | **Tailwind CSS 3 + shadcn/ui (Radix)** — acessibilidade WCAG 2.1 AA |
| Estado client | **TanStack Query 5** |
| Forms | **React Hook Form + Zod** |
| Auth | **NextAuth 4** com provider customizado para **Gov.br OAuth2** (Fase 9) |
| Gráficos | **Recharts** |
| i18n | pt-BR único por instância (município) |

O backend de UI usa **API Routes do Next.js** como BFF: chama os Spring services via rede interna (k8s ClusterIP), expõe ao browser apenas o que ele precisa. URLs internas em `apps/web/lib/services.ts`.

## Persistência

- **PostgreSQL 16** — extensões `pgcrypto` (campos sensíveis) e `pg_trgm` (busca textual)
- **Schemas:** `engine`, `financeiro`, `escolar`, `compliance` (init via `infra/docker/postgres/init.sql`)
- **Flyway:** apenas no `caq-engine-svc` (única autoridade de DDL); demais serviços usam `ddl-auto: validate`
- **Backup:** CronJob k8s → `pg_dump` → S3/MinIO com KMS

## Mensageria, cache, object store

| Componente | Escolha |
|---|---|
| Broker | **RabbitMQ 3.13** (vhost `caqi`) |
| Cache | **Redis 7** (sessões, rate-limit, TTL de catálogos) |
| Object store | **S3** (cloud) ou **MinIO** (on-prem/local) — anexos LAI, exports SIOPE, PDFs de relatórios |

## Observabilidade — Grafana stack (vendor-neutral)

| Sinal | Pipeline |
|---|---|
| Métricas | Micrometer → Prometheus (`/actuator/prometheus`) |
| Tracing | OpenTelemetry SDK → Tempo via OTLP (`OTEL_EXPORTER_OTLP_ENDPOINT`) |
| Logs | Logback JSON → Loki via Promtail |
| Painéis | Grafana (LGTM stack) |
| Alertas | Grafana Alerting → PagerDuty/Opsgenie/email |

## Hospedagem e infraestrutura

| Item | Escolha |
|---|---|
| Orquestração | **Kubernetes** (qualquer distro: EKS, AKS, GKE, OKE, Rancher, k3s) |
| Empacotamento | **Helm 3.13+** — chart `caqi` em `charts/caqi/` |
| Modelo de tenancy | **1 release por município** em namespace dedicado, com NetworkPolicy isolando |
| Ingress | **ingress-nginx** + **cert-manager** (Let's Encrypt) |
| Secrets | **HashiCorp Vault** (preferido) ou **External Secrets Operator** + cloud KMS |
| KMS | **AWS KMS** (default cloud) ou **Vault Transit** (on-prem) |
| Imagens | Registry corporativo (ECR/ACR/GCR/Harbor) — tag por release/git SHA |
| CD | **ArgoCD** (preferido) ou Flux — pull-based, GitOps |

## Compliance e segurança (camada cross-cutting)

| Mecanismo | Implementação |
|---|---|
| Logs imutáveis | Tabela `log_auditoria` (já no DDL) com chain SHA-256 (Merkle) — adulteração detectável offline |
| Criptografia em repouso | TDE no Postgres + `pgcrypto` para CPF e dados sensíveis |
| Criptografia em trânsito | TLS 1.3 obrigatório; mTLS entre serviços via Istio (Fase 9, opcional) |
| Auth & MFA | Gov.br OAuth2 + TOTP via NextAuth (Fase 9) |
| ROPA / LGPD | Tabela `ropa_registro` + UI gestão pelo DPO no `caq-compliance-svc` |
| Mascaramento PII | Camada DTO (MapStruct) com mappings `@Mask` |
| Regras legais | Domínio em `caq-compliance-svc` valida cada empenho/folha; bloqueia se viola 70%/15%/VAAR/MDE |

## Decisões tomadas — registradas

| Decisão | Valor | Reversibilidade |
|---|---|---|
| Java 21 + Spring Boot 3.3 | Sim | Alta — atualizações de minor são triviais |
| Gradle KTS + version catalog | Sim | Média — migração para Maven custaria 1 sprint |
| Microserviços já no MVP | 4 serviços + BFF | Média — extrair mais ou consolidar é viável |
| Schema-per-service em DB único | MVP | Alta — split em DBs separados é Fase 2 planejada |
| Next.js 14 App Router | Sim | Baixa — pivot para SPA exigiria reescrita do BFF |
| Helm + 1 release por município | Sim | Média — multi-tenant via RLS exigiria refatoração |
| Cloud-agnóstico (k8s) | Sim | Alta |
| Licença proprietária | Sim | Alta — pode ser relaxada para clientes específicos via contrato |

## Tech debt registrado

- [ ] Database-per-service real (Fase 2) — hoje todos os serviços apontam para a mesma DB
- [ ] Gradle wrapper não está no repo — usuário precisa rodar `gradle wrapper --gradle-version 8.10` uma vez
- [ ] Spring Cloud Gateway não foi adicionado — Ingress k8s cobre o caso no MVP; adicionar se houver necessidade de rate-limit/auth centralizado fora do BFF
- [ ] Library compartilhada (`platform/caq-shared-domain`) ainda não criada — DTOs entre serviços hoje seriam duplicados (entra na Fase 2)
- [ ] CI/CD não está no repo — GitHub Actions workflow vem na próxima Sprint
