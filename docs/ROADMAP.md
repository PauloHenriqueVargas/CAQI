# Roadmap de Entrega

Faseamento para evoluir do estado atual até v1.0 produtiva.

## Fase 0 — Fundação documental

**Status: ✓ Concluída.**

- [x] Repositório inicializado
- [x] Artefatos de Sprint 0 incorporados (OpenAPI, DDL, Postman, planilhas)
- [x] README, STACK, ROADMAP

## Fase 1 — Scaffold executável

**Status: ⏳ Em andamento.**

Sprint 1.A (este commit):
- [x] Stack definida e congelada — ver [STACK.md](STACK.md)
- [x] Gradle multi-project (root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`)
- [x] 4 microserviços Spring Boot scaffoldados (engine + 3 stubs):
  - `caq-engine-svc` com Health + CAQ controllers, DTOs, service, calculator stub
  - `caq-financeiro-svc`, `caq-escolar-svc`, `caq-compliance-svc` com Health + main class
- [x] Flyway no engine: V0001 derivada do DDL de Sprint 0 (26 tabelas)
- [x] Frontend Next.js 14 (App Router) com landing page, BFF skeleton, healthcheck
- [x] `docker-compose.yml` para dev local (postgres + redis + rabbitmq + minio + 4 svcs + web)
- [x] Helm chart base (`charts/caqi`) com `values.yaml`, `values-municipio-exemplo.yaml`, deployment-engine como template-modelo
- [x] LICENSE proprietária

Sprint 1.B (próximo commit):
- [ ] Templates Helm restantes (deployments dos outros serviços, Ingress, Secret, ConfigMap, ServiceAccount, NetworkPolicy)
- [ ] OpenAPI v0.2 — schemas completos (corrigir respostas vazias da v0.1.0)
- [ ] Library compartilhada `platform/caq-shared-domain` (DTOs entre serviços)
- [ ] CI mínima — `.github/workflows/ci.yml` (Gradle build+test, npm build+test, lint Helm, validação OpenAPI)
- [ ] Gradle wrapper commitado (eliminar passo manual)
- [ ] Smoke test E2E: subir docker-compose → curl health de cada serviço → green

**Definition of Done da Fase 1:** `docker compose up --build` sobe tudo; cada serviço responde 200 em `/api/v1/health` e `/actuator/health`; Next.js renderiza em http://localhost:3000; Flyway aplica V0001 sem erro; CI verde no PR de smoke test.

## Fase 2 — Motor CAQ/CAQi (Sprint 2–3)

Objetivo: cálculo CAQi e CAQ por etapa/escola/ano com memórias de cálculo auditáveis.

- [ ] Migrations adicionais: tabelas `calculo_caq_item` (memória de cálculo), `perfil_qualidade` (minimo/adequado), `custo_insumo_perfil` (custo por perfil)
- [ ] CRUD parâmetros por etapa/modalidade (`/api/v1/caqi/parametros`)
- [ ] CRUD insumos e custos com vigência (`/api/v1/caqi/insumos`, `/api/v1/caqi/custos`)
- [ ] Implementação real do `CaqCalculator`: matriz Insumo→Custo anualizada, por escola, por ano
- [ ] Indexação por SINAPI/IPCA (modelo `indice_preco`)
- [ ] Endpoint `/api/v1/caqi/calculos` retorna `R$/aluno/ano` + memória de cálculo (lista item-a-item)
- [ ] Persistência do cálculo em `calculo_caq` para histórico
- [ ] Relatório oficial: PDF + JSON com memória de cálculo
- [ ] **Teste de regressão:** comparar com `docs/references/Planilha_Base_CAQi_Preenchida.xlsx` (resultado bate ±0,5%)

## Fase 3 — Fundeb/MDE + Validador (Sprint 3–4) — `caq-financeiro-svc`

- [ ] Schema `financeiro`: receitas, despesas, folha, fontes (VAAF/VAAT/VAAR), naturezas PCASP
- [ ] CRUD básico via JPA
- [ ] Motor de validação no `caq-compliance-svc`: 70% pessoal, 15% VAAT capital, MDE 25%
- [ ] Bloqueio de empenho que viole regra (interceptor REST + evento RabbitMQ)
- [ ] Endpoints `/api/v1/fundeb/execucao` e `/fundeb/validacoes`

## Fase 4 — Compras/Contratos + Tributário (Sprint 4–5) — `caq-financeiro-svc`

- [ ] Lei 14.133: ETP, TR, matriz de risco, modalidades
- [ ] Integração PNCP (publicação de contratos via API)
- [ ] Motor de retenções: IRRF, INSS, ISS, PIS/Cofins/CSLL, DAS (Simples Nacional CGSN 140/2018)
- [ ] Endpoint `/api/v1/tributario/retencoes`

## Fase 5 — SIOPE Connector + Censo (Sprint 5–6) — `caq-compliance-svc`

- [ ] ETL Censo Escolar/INEP (download anual + import)
- [ ] Pré-preenchimento SIOPE com dados consolidados
- [ ] Validador de pendências
- [ ] Endpoints `/api/v1/siope/export` e `/siope/status`
- [ ] Analytics layer: dbt em `analytics/` (DuckDB → Postgres) para reports cruzados

## Fase 6 — Transparência/LAI (Sprint 6) — `caq-compliance-svc` + `web`

- [ ] Portal de dados abertos (rota pública sem auth no Next.js)
- [ ] API pública sob `/api/public/...` no compliance-svc
- [ ] Publicação automática (LRF 48-A) — CronJob k8s
- [ ] Repositório documental (anexos contratuais em S3/MinIO)

## Fase 7 — Auditoria + Simulador (Sprint 7) — `caq-compliance-svc` + `caq-engine-svc`

- [ ] Logs imutáveis com chain SHA-256 (`log_auditoria` já no schema)
- [ ] Painel CACS-Fundeb / CME (visões somente leitura com pareceres)
- [ ] Simulador "e se?" — expansão de tempo integral, redução alunos/turma, novas creches

## Fase 8 — Frontend admin completo (paralelo às fases 2–7) — `web`

- [ ] Auth NextAuth + sessões
- [ ] Dashboards executivos (prefeito/secretário) com Recharts
- [ ] Telas operacionais por escola
- [ ] Acessibilidade WCAG 2.1 AA (auditoria com axe-core)
- [ ] i18n se necessário (default pt-BR)

## Fase 9 — Hardening produção

- [ ] Auth Gov.br OAuth2 (NextAuth provider customizado + JWT trust no backend)
- [ ] MFA TOTP
- [ ] Spring Security em todos os controllers (RBAC)
- [ ] mTLS entre serviços (Istio/Linkerd, opcional)
- [ ] Pen-test interno
- [ ] DPIA / RIPD (LGPD) documentados
- [ ] Disaster recovery + backups (pg_dump → S3 com KMS)
- [ ] Documentação de operação (runbook por incidente)

## Cronograma indicativo

| Fase | Semanas | Entregável principal |
|---|---|---|
| 0 — Fundação | ✓ | Documentação aprovada |
| 1 — Scaffold | 2 | Build & run local funcionando |
| 2 — Motor CAQ | 3 | Cálculo CAQi/CAQ ✓ |
| 3 — Fundeb/MDE | 2 | Validador 70%/15%/25% ✓ |
| 4 — Compras/Tributário | 2 | Retenções + contratos ✓ |
| 5 — SIOPE/Censo | 2 | Export pré-validado ✓ |
| 6 — Transparência | 2 | Portal LAI público ✓ |
| 7 — Auditoria/Simulador | 2 | Logs + cenários ✓ |
| 8 — Frontend admin | 4 (paralelo) | UI completa ✓ |
| 9 — Hardening | 3 | Pronto para produção ✓ |

**Total**: ~19 semanas calendário, equipe pequena (3–5 devs) com paralelismo do frontend.

## Marcos de validação externa

- **M1** (fim da Fase 2): apresentação do motor CAQ ao Conselho Municipal de Educação
- **M2** (fim da Fase 4): demo Fundeb/Tributário ao controle interno
- **M3** (fim da Fase 6): homologação SIOPE (FNDE)
- **M4** (fim da Fase 7): apresentação ao TCE/CACS-Fundeb
- **M5** (fim da Fase 9): go-live piloto em município parceiro
