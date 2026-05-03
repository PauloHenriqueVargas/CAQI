# Stack Tecnológica — Proposta

Documento para alinhamento. Decisões tomadas aqui guiam todo o scaffold subsequente. **Status: proposta — aguarda validação.**

## Princípios

1. **Modular monolith primeiro, microserviços depois.** O prompt-mestre menciona microserviços, mas para o MVP prefere-se um monolito modular com fronteiras de domínio claras (módulos `caq_engine`, `orcamento`, `compras`, `escolar`, `pessoal`, `siope`, `transparencia`, `auditoria`, `fiscal`). Cada módulo expõe sua interface por contrato; quando um justificar deploy isolado (escala, equipe, ciclo), é extraído.
2. **API-first com OpenAPI 3.1.** A spec já existente em [`openapi/caqi-openapi.yaml`](../openapi/caqi-openapi.yaml) é a fonte da verdade — código deriva dela (geração de stubs).
3. **Conformidade auditável por construção.** Logs imutáveis em cadeia de hash (Merkle-style), mascaramento de PII em camada de serialização, enforcement de regras legais (MDE 25%, Fundeb 70%/15%, VAAR) no domínio — não na UI.
4. **Open source, padrões abertos, hospedagem soberana.** Compatibilidade com nuvens BR (Gov.br, Embratel, OCI BR) e on-premises municipal.

## Stack proposta

### Backend — API e domínio

| Camada | Escolha | Justificativa |
|---|---|---|
| Linguagem | **Python 3.12** | Ecossistema gov-tech maduro (dbt, Airflow, pandas, OpenPyXL p/ planilhas legais), produtividade alta, fácil contratação |
| Framework | **FastAPI** | Geração nativa OpenAPI 3.1, typing-first, async, performante; alinha com a spec já existente |
| ORM/Migrations | **SQLAlchemy 2 + Alembic** | DDL existente em [`apps/api/migrations/0001_initial_schema.sql`](../apps/api/migrations/0001_initial_schema.sql) é convertido para Alembic |
| Validação | **Pydantic v2** | Modelos compartilhados entre API, domínio e dbt sources |
| Auth | **Authlib + python-jose** | OAuth2 Gov.br + JWT local; MFA via TOTP (pyotp) |
| Filas/jobs | **Dramatiq + Redis** (ou Celery) | ETL SIOPE, geração de relatórios, publicação portal |
| Observabilidade | **OpenTelemetry + Prometheus** | Tracing distribuído; logs estruturados (structlog) |

### Banco de dados — transacional e analítico

| Camada | Escolha | Justificativa |
|---|---|---|
| OLTP | **PostgreSQL 16** | Conformidade contábil, JSONB para parametrizações flexíveis, RLS para multi-tenancy, extensões `pg_audit`/`pgcrypto` |
| OLAP / DW | **DuckDB + dbt-duckdb** (MVP) → **ClickHouse** ou **PostgreSQL particionado** (escala) | Motor CAQ e simulações rodam em colunar; DuckDB é zero-infra para começar |
| Cache / fila | **Redis 7** | Sessões, rate-limit, broker Dramatiq |
| Object store | **MinIO** (on-prem) ou S3-compat | Documentos LAI, anexos contratuais, exports SIOPE |

### Frontend

| Camada | Escolha | Justificativa |
|---|---|---|
| Framework | **React 18 + TypeScript + Vite** | Maturidade, contratação fácil, bundle pequeno |
| Roteamento/estado | **TanStack Router + TanStack Query** | Type-safe routing, cache de queries; sem Redux |
| UI | **shadcn/ui + Radix + Tailwind CSS** | Acessibilidade WCAG 2.1 AA por padrão (Radix), customização total |
| Gráficos | **Recharts** + **visx** (visualizações financeiras) | Painéis CAQ, execução Fundeb, simulações |
| Forms | **React Hook Form + Zod** | Validação compartilhada com Pydantic via JSON Schema |
| i18n | **i18next** (pt-BR padrão) | LGPD/LAI exigem conteúdo em português |

### Analítico / dbt

| Camada | Escolha | Justificativa |
|---|---|---|
| Transformação | **dbt-core 1.8+** com `dbt-duckdb` (dev) e `dbt-postgres` (prod) | Lineage, testes, docs; modelos `staging → intermediate → marts` |
| Marts | `marts/caq` (motor CAQi/CAQ), `marts/fundeb` (70%/15%/VAAR), `marts/transparencia` (LAI), `marts/siope` | Cada mart vira fonte de relatórios oficiais |
| Orquestração | **Dagster** (preferido) ou **Airflow** | Jobs SIOPE/INEP/SINAPI/IBGE; agendamento; observabilidade |
| Catálogo de dados | **DataHub** (futuro) | Compliance LGPD: data lineage e classificação de PII |

### Infraestrutura e DevOps

| Camada | Escolha | Justificativa |
|---|---|---|
| Containers | **Docker + docker-compose** (dev) | Stack reproduzível |
| Orquestração | **Kubernetes** (prod) ou **Docker Swarm** (entes pequenos) | Hospedagem soberana flexível |
| CI/CD | **GitHub Actions** | Lint, testes, build, deploy; pre-commit hooks (ruff, black, mypy, sqlfluff) |
| IaC | **Terraform** ou **Pulumi** | Provisionamento de nuvens BR |
| Secrets | **HashiCorp Vault** ou **SOPS** | LGPD: separação de chaves/dados |

### Compliance e segurança

| Mecanismo | Implementação |
|---|---|
| Logs imutáveis (LRF/LGPD) | Tabela `log_auditoria` (já existe) + chain de hash SHA-256 entre registros consecutivos |
| Criptografia em repouso | TDE no Postgres, KMS para chaves; campos sensíveis (CPF) com `pgcrypto` |
| Criptografia em trânsito | TLS 1.3 obrigatório, mTLS entre serviços internos |
| MFA | TOTP no fluxo local; herdado do Gov.br quando OAuth2 |
| ROPA | Tabela `ropa_registro` (já existe) + UI de gestão pelo DPO |
| Mascaramento PII | Camada Pydantic com decorators `@mask_pii`; views materializadas para BI |
| Regras legais (motor) | Domínio: cada empenho passa por `compliance.evaluate()` antes de persistir; bloqueia se violar 70%/15%/VAAR/MDE |

## Justificativas para validação

| Questão aberta | Sugestão | Alternativa |
|---|---|---|
| Python vs. Java/Kotlin? | Python (produtividade, ML, dbt) | Java Spring (mais comum em prefeituras grandes) |
| Monolito modular vs. microserviços? | Monolito modular (MVP) | Microserviços (depois) |
| DuckDB vs. ClickHouse no MVP? | DuckDB (zero-infra) | ClickHouse (se já houver cluster) |
| React vs. Next.js? | React + Vite (SPA, mais simples) | Next.js (se SSR/SEO no portal de transparência) |
| Multi-tenant SaaS ou single-tenant por município? | Multi-tenant com RLS no Postgres | Instâncias separadas (mais isolamento, mais custo) |

## Decisões pendentes (aguardam input)

- [ ] **Aprovar a stack acima** — ou apontar substituições.
- [ ] **Modelo de tenancy**: SaaS multi-tenant compartilhado (mais barato, RLS) ou instância por município (mais simples de auditoria, mais caro)?
- [ ] **Hospedagem alvo**: AWS BR, Azure BR, Google Cloud BR, OCI BR, on-prem municipal? Define escolha de KMS, object store, observabilidade.
- [ ] **Frontend separado vs. Next.js fullstack**: SPA + API ou SSR integrado?
- [ ] **Licença** do código: MIT, Apache 2.0, GPL-3.0, ou proprietária?

Após validação, este documento é congelado e a próxima fase começa: scaffold do FastAPI + Alembic + dbt + workflows CI.
