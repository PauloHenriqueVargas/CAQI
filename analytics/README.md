# Analytics — dbt

Camada analítica do Sistema CAQ/CAQi. Lê das tabelas operacionais do Postgres
(escritas pelos 4 microserviços Spring) e produz **marts** para BI/relatórios
do controle social, gestores e auditoria.

## Stack

- [dbt-core 1.8](https://docs.getdbt.com/) (Apache 2.0) + adapter `dbt-postgres`
- Padrão **staging → intermediate → marts** (single source of truth, refatorável)
- Schemas Postgres: `analytics_staging`, `analytics_intermediate`, `analytics_marts`
- Materialização: views em staging/intermediate, tables em marts (BI consome)

## Estrutura

```
analytics/
├── dbt_project.yml          # config principal
├── profiles.example.yml     # template — real fica em ~/.dbt/profiles.yml
├── packages.yml             # dbt_utils
├── requirements.txt         # dbt-core + dbt-postgres pinados
├── models/
│   ├── staging/             # 1:1 com tabelas operacionais (renomeia + casts)
│   ├── intermediate/        # agregações preparatórias
│   └── marts/
│       ├── caq_aluno_ano             # CAQi/CAQ por (escola, etapa, ano)
│       ├── fundeb_execucao_mensal    # %/cumpre por mês acumulado
│       └── transparencia_lai         # KPIs do portal público
├── seeds/                   # CSVs estáticos (não usado no MVP)
├── tests/                   # singular tests (custom SQL)
└── macros/                  # macros reutilizáveis
```

## Como rodar localmente

```bash
# 1. Subir o stack (postgres + serviços + seed dev)
docker compose up -d postgres

# 2. Aguardar Flyway aplicar migrações (caq-engine-svc faz no startup)
docker compose up -d caq-engine-svc

# 3. Setup do ambiente Python isolado para dbt
cd analytics
python3 -m venv .venv
source .venv/bin/activate     # Windows: .venv\Scripts\activate
pip install -r requirements.txt

# 4. Configurar profiles.yml
mkdir -p ~/.dbt
cp profiles.example.yml ~/.dbt/profiles.yml
# (ou exporte CAQI_DBT_HOST/USER/PASSWORD/DB)

# 5. Instalar deps (dbt_utils)
dbt deps

# 6. Rodar
dbt parse                     # valida sintaxe (sem DB)
dbt compile                   # compila para target/ (precisa DB)
dbt run                       # cria/atualiza views + tables
dbt test                      # roda assertions
dbt docs generate && dbt docs serve  # documentação browseable
```

## Ambiente de produção

Em prod, o ideal é apontar `dbt run` para uma **réplica analítica read-only**
(streaming logical replication ou snapshot diário) para isolar carga de
agregação do fluxo OLTP dos microserviços. Como MVP, rodar contra o mesmo
Postgres em horário de baixa carga já é aceitável.

Orquestração:
- **MVP**: `cron` simples (Helm CronJob `cronjob-dbt-run.yaml`) executando
  `dbt run --target prod` a cada 6h.
- **Defer**: Dagster/Airflow com observabilidade, retries, lineage.

## Convenções

- **Naming**: `stg_<tabela>` para staging, `int_<conceito>` para intermediate,
  `<dominio>_<grain>` para marts (sem prefixo).
- **Sufixos**: `_id` → BIGINT, `_codigo` → VARCHAR, `_ano` → INTEGER, `_competencia`
  → CHAR(6) `YYYYMM`.
- **Datas**: nunca strings em marts; sempre `DATE` ou `TIMESTAMP`. Em stg podem
  ainda estar como vieram (CHAR(6) etc.) — converter no intermediate.
- **Money**: `NUMERIC(14,2)` em todos os modelos. Nunca FLOAT (perde precisão).
- **Tests**:
  - Toda PK ganha `unique` + `not_null`
  - Toda FK ganha `relationships` para a tabela origem (em staging)
  - Marts têm `dbt_utils.expression_is_true` para invariantes (gap ≥ 0, % entre 0-200%)
- **Docs**: cada modelo tem `description:` no `_*.yml` correspondente

## Fontes (sources)

Declaradas em `models/staging/_sources.yml`. Ver schema operacional:
- `apps/caq-engine-svc/src/main/resources/db/migration/V0001..V0010__*.sql`
- `apps/caq-engine-svc/src/main/resources/db/seed/V9001..V9003__*.sql` (dados de exemplo)
