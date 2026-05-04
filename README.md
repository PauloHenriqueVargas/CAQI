# Sistema CAQ/CAQi

Sistema SaaS para gestão do **Custo Aluno Qualidade (CAQ/CAQi)** em redes municipais e estaduais de educação básica, com conformidade nativa às exigências legais brasileiras (CF/88, LDB, PNE, Fundeb, LRF, LAI, SIAFIC, Lei 14.133/2021, LGPD).

**Licença:** [Proprietária](LICENSE) — todos os direitos reservados.

## Arquitetura — visão rápida

- **Backend:** 4 microserviços Spring Boot 3 / Java 21 / Gradle KTS
  - `caq-engine-svc` (8081) — motor CAQ/CAQi, parâmetros, insumos, índices, simulações; **owner do schema PostgreSQL** via Flyway
  - `caq-financeiro-svc` (8082) — orçamento, Fundeb (70%/15%), MDE 25%, contabilidade SIAFIC/PCASP, retenções tributárias
  - `caq-escolar-svc` (8083) — escolas, turmas, matrículas, censo, PNAE/PNATE, pessoal/folha
  - `caq-compliance-svc` (8084) — validações legais, auditoria imutável (chain SHA-256), ROPA/LGPD, transparência LAI, integração SIOPE
- **Frontend + BFF:** [Next.js 14](apps/web) (App Router) + TypeScript + Tailwind + shadcn/ui — porta 3000
- **Persistência:** PostgreSQL 16 (schema-per-service no MVP; database-per-service em Fase 2)
- **Mensageria:** RabbitMQ
- **Object store:** S3 (cloud) ou MinIO (on-prem/local)
- **Observabilidade:** OpenTelemetry → Grafana stack (Loki + Tempo + Prometheus)
- **Deploy:** Helm chart `charts/caqi` — 1 chart, N releases (1 release por município, namespace dedicado)

Detalhes em [docs/STACK.md](docs/STACK.md). Faseamento em [docs/ROADMAP.md](docs/ROADMAP.md).

## Estrutura

```
CAQI/
├── apps/
│   ├── caq-engine-svc/         # Spring Boot — owner do schema
│   ├── caq-financeiro-svc/     # Spring Boot
│   ├── caq-escolar-svc/        # Spring Boot
│   ├── caq-compliance-svc/     # Spring Boot
│   └── web/                    # Next.js + BFF
├── charts/caqi/                # Helm chart (1 release por município)
├── analytics/                  # dbt (Fase 5+)
├── openapi/                    # spec OpenAPI 3.1 + Postman (Sprint 0)
├── infra/docker/               # init scripts (Postgres seed)
├── docs/                       # documentação, referências, conformidade
├── settings.gradle.kts         # Gradle multi-project
├── build.gradle.kts            # config compartilhada (Java 21, Spring BOM)
├── gradle/libs.versions.toml   # version catalog
├── docker-compose.yml          # dev local: postgres+redis+rabbitmq+minio+4 svcs+web
└── LICENSE                     # proprietária
```

## Quickstart — desenvolvimento local

**Pré-requisitos:** Docker + Docker Compose; Java 21; Node 20; Gradle 8.10 (ou use o wrapper).

### Bootstrap do Gradle Wrapper (uma vez)

O repositório ainda não inclui o `gradle/wrapper/gradle-wrapper.jar` nem `gradlew`. Gere-os com:

```bash
gradle wrapper --gradle-version 8.10
```

Depois disso, `./gradlew` substitui `gradle` em todos os comandos.

### Subir só a infra (Postgres + RabbitMQ + Redis + MinIO)

```bash
docker compose up -d postgres rabbitmq redis minio
```

### Rodar serviços nativos (recomendado em dev — hot reload)

```bash
# Em terminais separados:
./gradlew :apps:caq-engine-svc:bootRun
./gradlew :apps:caq-financeiro-svc:bootRun
./gradlew :apps:caq-escolar-svc:bootRun
./gradlew :apps:caq-compliance-svc:bootRun

cd apps/web && npm install && npm run dev
```

### Ou subir tudo via Docker

```bash
docker compose up --build
```

### Endpoints úteis

| URL | Descrição |
|---|---|
| http://localhost:3000 | Frontend (Next.js) |
| http://localhost:8081/swagger-ui.html | CAQ Engine |
| http://localhost:8082/swagger-ui.html | Financeiro |
| http://localhost:8083/swagger-ui.html | Escolar |
| http://localhost:8084/swagger-ui.html | Compliance |
| http://localhost:8081/actuator/health | Healthcheck (Spring Actuator) |
| http://localhost:15672 | RabbitMQ Management (caqi/caqi_dev_password) |
| http://localhost:9001 | MinIO Console (caqi_minio/caqi_minio_dev_password) |

### Tests

```bash
./gradlew test                            # todos os serviços
./gradlew :apps:caq-engine-svc:test       # um serviço
cd apps/web && npm test                   # Next.js
```

## Artefatos de Sprint 0 (preservados)

| Artefato | Local |
|---|---|
| OpenAPI 3.1 v0.1.0 | [openapi/caqi-openapi.yaml](openapi/caqi-openapi.yaml) |
| Postman collection | [openapi/postman/](openapi/postman/) |
| Modelo de dados (DDL inicial) | [apps/caq-engine-svc/src/main/resources/db/migration/V0001__initial_schema.sql](apps/caq-engine-svc/src/main/resources/db/migration/V0001__initial_schema.sql) |
| Prompt-mestre + anexo CAQ.pdf | [docs/references/](docs/references/) |
| Sprint 0 — stories + modelo | [docs/references/Sprint0_CAQ_Stories_ModeloDados.docx](docs/references/Sprint0_CAQ_Stories_ModeloDados.docx) |
| Planilha base CAQi | [docs/references/Planilha_Base_CAQi_Preenchida.xlsx](docs/references/Planilha_Base_CAQi_Preenchida.xlsx) |
| Checklist de conformidade | [docs/legal/Checklist_Conformidade_CAQ.xlsx](docs/legal/Checklist_Conformidade_CAQ.xlsx) |

## Status do desenvolvimento

| Fase | Estado |
|---|---|
| 0 — Fundação documental | ✓ Concluída |
| 1 — Scaffold executável | ⏳ **Em andamento** (este commit) |
| 2 — Motor CAQ/CAQi | Próxima |
| 3+ | Ver [docs/ROADMAP.md](docs/ROADMAP.md) |
