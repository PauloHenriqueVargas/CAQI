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

## Fase 2 — Motor CAQ/CAQi

**Status: ⏳ Em andamento.**

Sprint 2.A (concluída):
- [x] Migration V0002: adiciona `insumo.qtd_padrao` + tabela `calculo_caq_item` (memória) + índices
- [x] Migration V9001 (dev/test only, em `db/seed/`): seed do golden dataset da planilha — 6 etapas, 9 insumos com custos vigentes, 3 escolas, 1.250 matrículas, índices IPCA/SINAPI 2025
- [x] Locations Flyway profile-aware: `application-dev.yml` e `application-test.yml` carregam `db/migration` + `db/seed`
- [x] JPA entities (8): Etapa, ParametroEtapa, Insumo, CustoInsumo, Escola, Matricula, CalculoCaq, CalculoCaqItem
- [x] Repositories Spring Data com queries JPQL para vigência e contagem de matrículas
- [x] **Implementação real do `CaqCalculator`** — matriz Insumo→Custo anualizada com 3 modos (`por_aluno`/`por_turma`/`por_escola`), validação de vigência, memória item-a-item
- [x] `CaqService` persiste resultado em `calculo_caq` + itens em `calculo_caq_item`; idempotente (substitui cálculo anterior do mesmo trio escola+etapa+ano)
- [x] Endpoint `/api/v1/caqi/calculos` retorna `R$/aluno/ano` + memória de cálculo
- [x] **Teste de regressão** (`CaqCalculatorRegressaoTest`): para EF1 em Escola A 2025, valida cada item da memória contra os 6 itens da planilha exemplo (PES-001=3.400, MOB-001=280, MAT-001=350, SER-001=100, TEC-001=6, MAN-001=33,33) + total agregado R$ 4.440,17 (com os 3 itens adicionais não listados na planilha exemplo)

Sprint 2.B.i (concluída):
- [x] V0003: ALTER custo_insumo ADD perfil (CHECK minimo|adequado) + índice (insumo, perfil, vigência)
- [x] V0004: ALTER calculo_caq_item ADD perfil
- [x] V9002 (dev/test seed): 9 custos para perfil='adequado' com valores ilustrativos (~50-180% maiores que minimo)
- [x] CaqCalculator atualizado: itera os 2 perfis e devolve CAQi + CAQ + gap simultaneamente
- [x] ItemMemoriaCalculoDto + CalculoCaqItem ganham campo `perfil`
- [x] Test de regressão valida CAQi (R$ 4.440,17), CAQ (R$ 6.876,83) e gap (R$ 2.436,66) para EF1 em Escola A 2025
- [x] CRUD endpoints REST com Springdoc:
  - GET /api/v1/caqi/insumos · GET /{codigo} · POST · PUT
  - GET /api/v1/caqi/insumos/{codigo}/custos · POST (cria nova vigência)
  - GET /api/v1/caqi/parametros (vigentes) · GET /etapa/{etapaCodigo}
  - GET /api/v1/caqi/calculos · GET /{id} (com memória completa)
- [x] RestExceptionHandler global com RFC 7807 ProblemDetail (404, 400, validação)

Sprint 2.B.ii (concluída):
- [x] **Spring Security** — HTTP Basic Auth + InMemoryUserDetailsManager com 3 roles (LEITOR/GESTOR/ADMIN); RBAC declarativo em SecurityConfig (single source of truth) — ADMIN cria/edita insumos+parametros, GESTOR executa cálculos+novas vigências de custo, LEITOR consulta. Endpoints públicos: /api/v1/health, /actuator/health/**, swagger-ui. Senhas via env `CAQI_*_PASSWORD`. **Em prod: migrar para Gov.br OAuth2 (Fase 9).**
- [x] **Indexador SINAPI/IPCA** — IndicePreco entity + IndicePrecoRepository + IndexadorService.custoAtualizado(custo, dataRef): aplica fator acumulado ∏(1 + var/100) entre o mês posterior à vigência e a data de referência. CaqCalculator agora usa custo atualizado em vez do raw. Memória registra reajuste no `baseCalculo`. Test unitário (mock) cobre 5 cenários: sem índice, vigência cobre data, 12 meses IPCA acumulado, deflação, sem variações cadastradas.
- [x] **Eventos RabbitMQ** — CalculoExecutadoEvent (record) publicado no topic exchange `caqi.events` com routing key `caqi.calculo.executado.{municipioId}`. RabbitConfig + CaqEventPublisher (@ConditionalOnProperty `caqi.events.enabled`). CaqService publica fire-and-forget após persist (TODO Fase 9: trocar por TransactionalEventListener AFTER_COMMIT). Test profile desliga eventos.
- [x] OpenAPI atualizada (springdoc) com SecurityScheme Basic Auth — Swagger UI mostra cadeado e permite login.

Sprint 2.B.iii (próxima):
- [ ] POST/PUT em parametros/etapas (operação delicada — afeta cálculos retroativos)
- [ ] Relatório oficial: PDF do cálculo com memória (openhtmltopdf)
- [ ] TransactionalEventListener AFTER_COMMIT (eventos só após commit do BD)
- [ ] Tests de integração HTTP via MockMvc com @WithMockUser para validar RBAC

## Fase 3 — Fundeb/MDE + Validador

**Status: ⏳ Em andamento.**

Sprint 3.A (concluída) — `caq-financeiro-svc`:
- [x] JPA entities Receita, Despesa, FonteRecurso (mapeadas para tabelas existentes em V0001 schema public)
- [x] Repositories com queries JPQL agregadas: somar por origem, por siope_grupo, pessoal por fonte (natureza LIKE '3.1%'), capital por fonte (natureza LIKE '4%')
- [x] FundebService.calcular(ano) — retorna ExecucaoFundebDto com receitas/despesas consolidadas + 3 percentuais (MDE/Fundeb70/VAAT15) + 3 booleans cumpre*
- [x] CRUD endpoints com Springdoc:
  - GET/POST /api/v1/financeiro/receitas (filtro por ?ano=)
  - GET/POST /api/v1/financeiro/despesas (filtro por ?ano=)
  - GET/POST /api/v1/financeiro/fontes-recurso
  - GET /api/v1/fundeb/execucao?ano=2025
- [x] Spring Security HTTP Basic + RBAC declarativo (LEITOR/GESTOR/ADMIN — mesmo modelo do engine-svc)
- [x] V9003 (db/seed dev/test): financeiro reproduzível — 5 receitas + 6 despesas → cenário cumpre os 3 limites
- [x] FundebServiceTest unitário (Mockito): cenário-base do seed, violação Fundeb 70%, divisão por zero
- [x] application.yml: removido `default_schema: financeiro` (DDL ainda no public — split em Fase 2.II)

Sprint 3.B (concluída) — `caq-compliance-svc`:
- [x] V0005 (engine-svc/db/migration): tabela notificacao com payload JSONB + índices (status/ano, tipo, created_at)
- [x] JPA entities Notificacao + LogAuditoria (sobre tabelas existentes)
- [x] **AuditChainService**: chain SHA-256 (Merkle) sobre log_auditoria com lock pessimista (FOR UPDATE) no último log para serializar inserts concorrentes; verificarIntegridade() recalcula e devolve id do primeiro log quebrado (ou Optional.empty)
- [x] **AvaliadorComplianceService**: chama caq-financeiro-svc via REST (FinanceiroClient com Basic Auth), aplica regras MDE 25% / Fundeb 70% / VAAT 15% e cria Notificação (severidade `critica` para CF/Fundeb, `alta` para VAAT) + chain log a cada notificação
- [x] **RabbitMQ listener**: queue `caqi.compliance.calculo-executado.{municipioId}` bindada ao exchange `caqi.events` com routing key `caqi.calculo.executado.{municipioId}`. Recebe CalculoExecutadoEventDto, registra na cadeia de auditoria
- [x] Endpoints REST com Spring Security + RBAC (LEITOR/GESTOR/ADMIN):
  - POST /api/v1/compliance/avaliar?ano=2025 (GESTOR)
  - GET  /api/v1/compliance/notificacoes?status=aberta&ano=2025 (LEITOR)
  - PATCH /api/v1/compliance/notificacoes/{id}/status?novo=resolvida (GESTOR)
  - GET  /api/v1/compliance/auditoria (LEITOR)
  - GET  /api/v1/compliance/auditoria/verificar (LEITOR)
- [x] AuditChainServiceTest unit: hash determinístico hex 64, avalanche em qualquer campo, GENESIS=64 zeros

Sprint 3.C (concluída):
- [x] **Shared lib `platform/caq-shared-domain`**: CalculoExecutadoEvent + ExecucaoFundebDto records puros (sem dependências); java-library plugin; settings.gradle.kts inclui o módulo; engine, financeiro e compliance dependem com `implementation(project(":platform:caq-shared-domain"))`. Consequência: produtor e consumidor RabbitMQ usam o mesmo FQN — Jackson TypeId casa.
- [x] Removidos 4 DTOs duplicados (engine/api/dto/CalculoExecutadoEvent, financeiro/api/dto/ExecucaoFundebDto, compliance/api/dto/CalculoExecutadoEventDto, compliance/api/dto/ExecucaoFundebDto). Imports atualizados em 8 arquivos.
- [x] **Bloqueador de empenho** (`BloqueadorEmpenhoService`): persiste a despesa, faz flush, recalcula execução e detecta transição cumpre TRUE→FALSE via `AnalisadorImpactoFundeb` (função pura, testável). Quando flag `caqi.compliance.bloquear-empenhos-violadores=true` (default false), lança `EmpenhoBloqueadoException` (409 Conflict) — `@Transactional` faz rollback do INSERT.
- [x] DespesasController.criar() agora delega ao bloqueador.
- [x] Test unit `AnalisadorImpactoFundebTest`: 5 cenários (sem transição, MDE/Fundeb/VAAT cada um derrubado, já violado antes não-detecta).

Sprint 3.D (próxima — opcional antes de Fase 4):
- [ ] Cross-svc smoke test E2E (script bash que sobe docker-compose, POST despesa violadora com flag on, espera 409)
- [ ] Endpoint /api/v1/fundeb/validacoes (consolida notificações + prazos legais)

## Fase 4 — Compras/Contratos + Tributário — `caq-financeiro-svc`

**Status: ⏳ Em andamento.**

Sprint 4.A (concluída) — Motor de Retenções:
- [x] **MotorRetencoes** (stateless): aplica regras simplificadas alinhadas a Lei 9.430/1996, IN RFB 1.234/2012, Lei 8.212/1991, LC 123/2006:
  - Não-optante: IRRF 1,5% + (INSS 11% se serviço com cessão MO) + PIS 0,65% + COFINS 3,0% + CSLL 1,0% (se valor > R$ 215,05) + ISS conforme alíquota municipal
  - Simples Nacional: SÓ ISS retido (regra geral)
  - Lista de serviços com cessão MO: LIMPEZA_CONSERVACAO, ENGENHARIA, VIGILANCIA, TRANSPORTE_CARGAS, MANUTENCAO_PREDIAL, OBRAS_CIVIS
- [x] DTOs RequisicaoRetencaoDto + ResultadoRetencaoDto + ItemRetencaoDto (memória item-a-item com base legal)
- [x] Endpoint POST /api/v1/tributario/retencoes (GESTOR)
- [x] SecurityConfig: + RBAC para /api/v1/tributario/**
- [x] MotorRetencoesTest: 5 cenários (não-optante GERAL R$10k, com cessão MO, Simples só ISS, valor abaixo limite PCC, sem alíquota ISS)

Sprint 4.B (concluída) — Compras/Contratos Lei 14.133:
- [x] JPA entities Fornecedor, Contrato, MedicaoContrato (sobre tabelas existentes em V0001)
- [x] CRUD endpoints REST com Springdoc + RBAC declarativo:
  - GET / GET /{id} / POST / PUT /api/v1/financeiro/fornecedores (POST GESTOR; PUT ADMIN)
  - GET / GET /{id} / POST /api/v1/financeiro/contratos (POST ADMIN — estrutural)
  - GET / POST /api/v1/financeiro/contratos/{id}/medicoes (POST GESTOR — operacional)
- [x] Modalidade Lei 14.133 validada via @Pattern (PREGAO_ELETRONICO, CONCORRENCIA, DISPENSA, INEXIGIBILIDADE, DIALOGO_COMPETITIVO, CONCURSO, LEILAO)
- [x] **MedicaoComRetencaoService**: ao registrar medição, se tipoServico+aliquotaIssMunicipal informados, chama MotorRetencoes usando `optanteSimples` do fornecedor (fonte única) e devolve `MedicaoComRetencoesDto` com preview de retenções para emissão da guia (não persiste retenção)
- [x] MedicaoComRetencaoServiceTest unit (Mockito): 4 cenários — sem params (não calcula), não-optante (1115 retido), Simples (500 só ISS), contrato inexistente (404)

Sprint 4.C (próxima — defer):
- [ ] ETP, TR, matriz de risco como anexos JSONB no contrato
- [ ] Cliente HTTP para PNCP (publicação automática + sincronização de status)
- [ ] Persistência da retenção em retencao_tributaria quando despesa é paga
- [ ] Validador de saldo contratual (impede medição que estoura valor_global)

## Fase 5 — SIOPE Connector + Censo

**Status: ⏳ Em andamento.**

Sprint 5.A (concluída) — SIOPE export consolidado em `caq-financeiro-svc`:
- [x] **SiopeService.gerar(ano)**: agrega receitas por origem + despesas por (siope_grupo, fonte_recurso, classe pessoal/capital/outras), monta vinculações via FundebService, detecta pendências (receita sem origem/PCASP, despesa sem siope_grupo/natureza/fonte, capital fora de VAAT como info)
- [x] DTOs: SiopeExportDto + ReceitasResumo + DespesasResumo + Vinculacoes + VinculacaoDetalhe + SiopePendenciaDto
- [x] Endpoints REST com Springdoc + RBAC LEITOR:
  - GET /api/v1/siope/export?ano=N → quadro completo
  - GET /api/v1/siope/status?ano=N → apenas pendências (subset)
- [x] SiopeServiceTest unit (Mockito): 2 cenários — cenário-base seed (260k MDE / 230k pessoal / 10k capital, zero pendências) e cenário com falhas de classificação (5 pendências detectadas com severidades corretas)
- [x] Limitação documentada: FNDE não tem API pública para upload — service produz JSON; conversão para layout DFCD/MIM-CC fica para 5.D se necessário

Sprint 5.B (próxima — defer):
- [ ] Censo Escolar/INEP — cliente HTTP para download do arquivo anual + parser CSV/XLS
- [ ] Import para escolas/matriculas no caq-escolar-svc
- [ ] Sincronização com cálculo CAQ (engine recalcula quando matrículas atualizam)

Sprint 5.C (próxima — defer):
- [ ] Analytics layer dbt — modelos staging/intermediate/marts em `analytics/`
- [ ] Marts: caq_aluno_ano, fundeb_execucao_mensal, transparencia_lai
- [ ] Orquestração via Dagster ou cron simples

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
