# Roadmap de Entrega

Faseamento proposto para evoluir do estado atual (artefatos de Sprint 0) até a v1.0 produtiva.

## Fase 0 — Fundação (atual)

Status: **em andamento.**

- [x] Repositório inicializado (git, estrutura de diretórios)
- [x] Artefatos de Sprint 0 incorporados (OpenAPI, DDL, Postman, planilhas, checklist)
- [x] Documentos fundacionais: README, STACK, ROADMAP
- [ ] Validação da stack pelo time (ver [STACK.md](STACK.md))
- [ ] ARCHITECTURE.md detalhada (C4 — context, container, component)
- [ ] COMPLIANCE.md — matriz lei → módulo → regra automatizada → evidência
- [ ] CAQ_ENGINE.md — fórmulas, parâmetros, validações

## Fase 1 — Scaffold executável (Sprint 1, ~2 semanas)

Objetivo: build & run local funcionando, com health check e contrato OpenAPI espelhado em código.

- [ ] `apps/api/` — FastAPI scaffold com módulos vazios (caq_engine, orcamento, compras, escolar, pessoal, siope, transparencia, auditoria, fiscal)
- [ ] `apps/api/migrations/` — Alembic configurado, primeira migration deriva do DDL existente
- [ ] OpenAPI v0.2 — schemas completos para todas as respostas (corrigir o que está vazio na v0.1.0)
- [ ] `analytics/` — dbt-duckdb scaffold com sources, modelo `staging`, primeiro modelo `marts/caq/caq_calculo`
- [ ] `docker-compose.yml` — postgres + redis + minio + api
- [ ] CI mínima (GitHub Actions): lint Python, testes pytest, lint dbt, validação OpenAPI
- [ ] Seed de dados — converter `Planilha_Base_CAQi_Preenchida.xlsx` em SQL/CSV para fixtures

**Definition of Done:** `docker compose up` sobe o stack, `GET /health` responde 200, dbt roda `dbt build` sem erros sobre dados seed.

## Fase 2 — Motor CAQ/CAQi (Sprint 2–3)

Objetivo: cálculo CAQi e CAQ por etapa/escola/ano com memórias de cálculo auditáveis.

- [ ] CRUD parâmetros por etapa/modalidade (`/caqi/parametros`)
- [ ] CRUD insumos e custos com vigência (`/caqi/insumos`)
- [ ] Endpoint `/caqi/calculos` com matriz Insumo→Custo anualizada
- [ ] Indexação por SINAPI/IPCA (modelo `indice_preco`)
- [ ] dbt: `marts/caq/caq_aluno_ano` (R$/aluno/ano por etapa × escola)
- [ ] Relatório oficial: CAQi/CAQ por etapa com memória de cálculo (PDF + JSON)
- [ ] Testes: comparação com `Planilha_Base_CAQi_Preenchida.xlsx` (resultado deve bater)

**DoD:** dado o seed da planilha de referência, o motor reproduz os mesmos valores ±0,5%.

## Fase 3 — Fundeb/MDE + Validador (Sprint 3–4)

- [ ] Modelo orçamentário (fontes VAAF/VAAT/VAAR, naturezas PCASP)
- [ ] CRUD receitas, despesas, folha (já há tabelas)
- [ ] Motor de validação: 70% pessoal, 15% VAAT capital, MDE 25%
- [ ] Bloqueio de empenho que viole regra (interceptor no domínio)
- [ ] Endpoint `/fundeb/execucao` e `/fundeb/validacoes` com dados reais
- [ ] dbt: `marts/fundeb/fundeb_execucao_mensal`

## Fase 4 — Compras/Contratos + Tributário (Sprint 4–5)

- [ ] Lei 14.133: ETP, TR, matriz de risco, modalidades
- [ ] Integração PNCP (publicação de contratos)
- [ ] Motor de retenções: IRRF, INSS, ISS, PIS/Cofins/CSLL, DAS (Simples)
- [ ] Endpoint `/tributario/retencoes` com cálculo correto

## Fase 5 — SIOPE Connector (Sprint 5–6)

- [ ] ETL Censo Escolar/INEP (download + import)
- [ ] Pré-preenchimento SIOPE com base nos dados consolidados
- [ ] Validador de pendências
- [ ] Endpoint `/siope/export` (arquivo SIOPE-ready) e `/siope/status`

## Fase 6 — Transparência/LAI (Sprint 6)

- [ ] Portal de dados abertos (frontend público)
- [ ] API pública sem auth para dados não sensíveis
- [ ] Publicação automática (LRF 48-A) — cron de publicação
- [ ] Repositório documental (anexos contratuais, atos)

## Fase 7 — Auditoria + Simulador (Sprint 7)

- [ ] Logs imutáveis com chain SHA-256
- [ ] Painel CACS-Fundeb / CME (visões somente leitura com pareceres)
- [ ] Simulador "e se?" — expansão de tempo integral, redução alunos/turma, novas creches

## Fase 8 — Frontend administrativo (paralelo às fases 2–7)

- [ ] React + Vite scaffold, layout, auth
- [ ] Dashboards executivos (prefeito/secretário)
- [ ] Telas operacionais por escola
- [ ] Acessibilidade WCAG 2.1 AA (auditoria com axe)

## Fase 9 — Hardening produção

- [ ] Auth Gov.br OAuth2
- [ ] MFA TOTP
- [ ] Pen-test interno
- [ ] DPIA / RIPD (LGPD)
- [ ] Disaster recovery + backups
- [ ] Documentação de operação

## Cronograma indicativo

| Fase | Semanas | Entregável principal |
|---|---|---|
| 0 — Fundação | 1 | Documentação aprovada |
| 1 — Scaffold | 2 | API/dbt/CI rodando |
| 2 — Motor CAQ | 3 | Cálculo CAQi/CAQ ✓ |
| 3 — Fundeb/MDE | 2 | Validador 70%/15%/25% ✓ |
| 4 — Compras/Tributário | 2 | Retenções + contratos ✓ |
| 5 — SIOPE | 2 | Export pré-validado ✓ |
| 6 — Transparência | 2 | Portal LAI público ✓ |
| 7 — Auditoria/Simulador | 2 | Logs + cenários ✓ |
| 8 — Frontend admin | 4 (paralelo) | UI completa ✓ |
| 9 — Hardening | 3 | Pronto para produção ✓ |

**Total**: ~19 semanas calendário de trabalho focado de uma equipe pequena (3–5 devs), considerando paralelismo do frontend.

## Marcos de validação externa

- **M1** (fim da Fase 2): apresentação do motor CAQ ao Conselho Municipal de Educação
- **M2** (fim da Fase 4): demo Fundeb/Tributário ao controle interno
- **M3** (fim da Fase 6): homologação SIOPE
- **M4** (fim da Fase 7): apresentação ao TCE/CACS-Fundeb
- **M5** (fim da Fase 9): go-live piloto em município parceiro
