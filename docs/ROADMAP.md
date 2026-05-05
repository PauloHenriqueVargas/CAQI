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

Sprint 5.B (concluída) — Censo Escolar/INEP no `caq-escolar-svc`:
- [x] **V0009** — `censo_importacao` (audit trail por hash) + `censo_matricula_resumo` (count por escola/etapa/ano) + partial unique index em `escola(inep_id) WHERE inep_id IS NOT NULL`
- [x] **CensoEscolarParser** com Jackson `CsvMapper` em **streaming** (não carrega arquivo em memória) — Latin-1 (ISO-8859-1), separador `;`, header inference; calcula SHA-256 e conta bytes em paralelo via `DigestInputStream` + `FilterInputStream`. Tolera colunas faltando ou em ordem diferente (mixin `@JsonIgnoreProperties(ignoreUnknown=true)`)
- [x] **CensoEscolaRecord** record — subset do layout INEP (NU_ANO_CENSO, CO_ENTIDADE, NO_ENTIDADE, CO_MUNICIPIO, TP_DEPENDENCIA, TP_LOCALIZACAO, TP_SITUACAO_FUNCIONAMENTO, QT_MAT_INF_CRE/PRE, QT_MAT_FUND_AI/AF, QT_MAT_MED, QT_MAT_EJA, QT_MAT_PROF) + helpers `ehMunicipal/emAtividade/localizacaoTexto`
- [x] **CensoEtapaMapper** — tradução QT_MAT_* → códigos canônicos do engine: CRECHE/PRE/EF1/EF2/EM/EJA/PROF
- [x] **CensoImportService** transacional:
  - Filtra por `cod-municipio-ibge` (tenant config) + `TP_DEPENDENCIA=3` (Municipal) + `TP_SITUACAO=1` (em atividade)
  - Idempotência por `(ano + sha256 + cod_municipio_ibge)` — re-import retorna resultado anterior sem reescrever
  - Upsert escola por `inep_id` (atualiza nome/rede/localizacao/situacao)
  - Insere `censo_matricula_resumo` por (escola, etapa, ano) só quando `qtd_alunos > 0`
  - Modo `dryRun`: parse + filtro acontecem mas nada é persistido (status `simulada`)
  - Bloqueia se `cod-municipio-ibge` for inválido (não-7-dígitos ou `0000000`)
- [x] **CensoController** — `POST /import` (multipart, ADMIN), `POST /dry-run` (multipart, GESTOR), `GET /importacoes` + `GET /importacoes/{id}` (LEITOR); tudo Springdoc-anotado
- [x] **SecurityConfig** + **SecurityProperties** (3 roles LEITOR/GESTOR/ADMIN, mesmo modelo do engine/financeiro), Basic Auth + RBAC declarativo, endpoints públicos (health, swagger)
- [x] **TenantProperties** + `caqi.tenant.cod-municipio-ibge` config (env `CAQI_COD_MUNICIPIO_IBGE`)
- [x] `application.yml`: removido `default_schema: escolar`; adicionado `spring.servlet.multipart.max-file-size=200MB` (Censo INEP típico 30-80MB)
- [x] Lib **`com.fasterxml.jackson.dataformat:jackson-dataformat-csv`** adicionada via version catalog
- [x] **CensoEscolarParserTest** (7 testes): parse fixture 8 registros + hash determinístico + idempotência hash + acentos Latin-1 preservados + helpers + CSV mínimo + stream vazio (SHA-256 e3b0...)
- [x] **CensoImportServiceTest** (7 testes Mockito): import completo (4 escolas, 1460 matrículas, 10 resumos), agregação por etapa (CRECHE=185 PRE=350 EF1=500 EF2=410 EJA=15), dry-run não persiste, idempotência retorna sem reescrever, upsert por inep_id, cod-ibge inválido falha, município ≠ alvo zera filtrados
- [x] **Fixture** `src/test/resources/censo/ESCOLAS_FIXTURE.csv` — 8 escolas representando todos os caminhos de filtro (municipal/estadual/privada/outro município/extinta/rural)

Sprint 5.B (defer):
- [ ] Cliente HTTP para download automático do ZIP anual INEP (`https://download.inep.gov.br/dados_abertos/microdados_censo_escolar_*.zip`) com unzip streaming + retry — atualmente o operador faz upload manual via UI
- [ ] Importer para DOCENTES_*.csv → `pessoa_servidor`
- [ ] Sincronização CAQ: trigger evento `caqi.censo.importado.{municipioId}` consumido pelo engine para recalcular CAQ das escolas afetadas (decisão pendente: usar contagens Censo como fallback quando `matricula` operacional não estiver populada?)

Sprint 5.D (concluída) — Microdados aluno-level (MATRICULA_*.csv) com pseudonimização LGPD:
- [x] **V0012** cria `censo_matricula` (BIGSERIAL, FK importacao + escola, ano, id_matricula_inep, **id_aluno_hash CHAR(64)**, escola_inep, etapa_codigo, tp_etapa_ensino, idade, tp_sexo, tp_cor_raca, tp_zona_residencial, in_necessidade_especial, necessidades_codigos TEXT) + 4 índices (escola, ano+etapa, demografia, NEE-only partial); estende `censo_importacao` com `subtipo_arquivo` e `matriculas_inseridas`
- [x] **`CensoMatriculaRecord`** record com 20 campos do layout INEP + helpers `ehDependenciaMunicipal()`, `inNecessidadeEspecialBool()`, `necessidadesCsv()` (concatena IN_CEGUEIRA/SURDEZ/AUTISMO/etc. em CSV legível)
- [x] **`CensoEtapaEnsinoMapper`** — tradução TP_ETAPA_ENSINO (1-77) → códigos canônicos do engine (CRECHE/PRE/EF1/EF2/EM/EJA/PROF), retornando null para etapas não relevantes (matrícula descartada)
- [x] **`CensoMatriculaParser`** — Latin-1 + `;` + streaming + SHA-256 + bytes, mesmo padrão do `CensoEscolarParser` (compartilha `CENSO_CHARSET` e `CENSO_SEPARATOR`)
- [x] **`CensoMatriculaImportService`** transacional:
  - Filtra `cod_municipio_ibge` + `TP_DEPENDENCIA_ADM=3` + etapa mapeável
  - **Pseudonimiza `ID_ALUNO`** via `SHA-256(id || salt)` onde salt vem de `caqi.censo.pseudonimizacao-salt` (≥8 bytes, validado; rejeita salt curto)
  - **Batch insert** de 1000 records — suporta arquivos com milhões de linhas sem estouro de heap
  - Cache de `escola_id` por `inep_id` evita N selects no loop
  - Modo `dryRun` computa estatísticas sem persistir
  - Acumulador in-memory para `resumoMatriculas` por etapa
- [x] **`CensoController`**: `POST /import-matriculas` (ADMIN), `POST /dry-run-matriculas` (GESTOR) — extrai helper `preExec()` para deduplicar validação ano/username/nome
- [x] **SecurityConfig**: + RBAC `/import-matriculas` ADMIN, `/dry-run-matriculas` GESTOR
- [x] `application.yml`: `caqi.censo.pseudonimizacao-salt` config; `multipart.max-file-size=2GB` (MATRICULA Brasil-inteiro pode chegar a ~5GB; recomendado dividir por região)
- [x] **DPIA atualizado** (§3 + §3.2): tabela `censo_matricula` listada como dado sensível pseudonimizado de criança/adolescente; documenta hipótese de tratamento (LGPD art. 11 §1° I+IV — políticas públicas de educação) + obrigatoriedade de rotação do salt em incidente
- [x] **`CensoMatriculaImportServiceTest`** (9 cenários Mockito): import com fixture 12 linhas filtra para 8 válidas (CRECHE=2 EF1=3 EF2=1 EM=1 EJA=1, NEE=2 com DEFICIENCIA_INTELECTUAL+AUTISMO); pseudonimização hex 64-char nunca em claro; salt diferente gera hash diferente (rotação); NEE flags codificadas corretamente; dry-run não persiste; salt curto rejeitado; cod-IBGE inválido rejeitado; mapeamento de 7 etapas + null para 77 e null
- [x] **Fixture** `MATRICULA_FIXTURE.csv` com 12 alunos representando todos os caminhos: 2 creches + 3 EF1 + 1 EF2 + 1 EM + 1 EJA municipais Palmas + 1 estadual + 1 privada + 1 outro município + 1 etapa não mapeada

Sprint 5.C (concluída) — Camada analítica dbt em `analytics/`:
- [x] **Skeleton dbt-core 1.8 + dbt-postgres** — `dbt_project.yml`, `profiles.example.yml`, `packages.yml` (dbt_utils 1.3), `requirements.txt` (versões pinadas), `.gitignore`, `README.md` com setup local e produção
- [x] Convenção de schemas: `analytics_staging` + `analytics_intermediate` (views) + `analytics_marts` (tables — BI consome aqui); `+persist_docs` para descrições gravadas no Postgres
- [x] **Sources** declarados em `models/staging/_sources.yml` para 16 tabelas operacionais (V0001..V0010): escola, etapa, parametro_etapa, insumo, custo_insumo, indice_preco, calculo_caq, calculo_caq_item, receita, despesa, fonte_recurso, fornecedor, contrato, medicao_contrato, notificacao, log_auditoria, publicacao_portal, censo_importacao, censo_matricula_resumo
- [x] **13 staging models** (`stg_*.sql`) — 1:1 com tabelas operacionais com renames + casts + flags derivadas:
  - `stg_escola/etapa/insumo/custo_insumo` (catálogo)
  - `stg_calculo_caq` renomeia `gap_execucao` → `gap_adequado` (alinha vocabulário)
  - `stg_calculo_caq_item` (memória item-a-item, perfil minimo|adequado)
  - `stg_receita` + `stg_despesa` extraem ano/mes/competencia_date da CHAR(6) YYYYMM; despesa ganha `is_pessoal` (3.1.x) e `is_capital` (4.x)
  - `stg_fonte_recurso/fornecedor/contrato/medicao_contrato` (compras)
  - `stg_notificacao` calcula `dias_para_resolver`
  - `stg_publicacao_portal` (LRF 48-A trail)
  - `stg_log_auditoria` (chain SHA-256)
  - `stg_censo_matricula_resumo` (microdados INEP agregados)
- [x] **`_staging.yml`** com testes em todos os PKs (unique + not_null), FKs (relationships), valores de domínio (accepted_values para tipo_aplicacao, perfil, severidade, status, modalidade Lei 14.133), ranges (mes 1-12)
- [x] **3 intermediate models**:
  - `int_receitas_anuais_por_origem` — soma por (ano, origem) + flags eh_mde_base/eh_fundeb_base/eh_vaat
  - `int_despesas_classificadas` — JOIN com fonte_recurso + flags classe_pessoal_fundeb/classe_capital_vaat/eh_mde
  - `int_calculo_perfil_pivoted` — pivota itens por perfil (mínimo vs adequado) lado-a-lado
- [x] **3 mart models** (table materialized + indexes):
  - **`caq_aluno_ano`** — grain (escola, etapa, ano); 1 linha por calculo; campos desnormalizados (escola_nome, etapa_codigo) + métricas derivadas (pct_gap, pct_pessoal_no_caqi, num_itens_minimo/adequado)
  - **`fundeb_execucao_mensal`** — grain (ano, mes); calendário 12×anos com window function `sum() over (partition by ano order by mes rows unbounded preceding to current)` para acumulado; pcts MDE/Fundeb70/VAAT15 + booleans cumpre_*; permite ver trajetória mensal, não só fim de ano
  - **`transparencia_lai`** — grain (ano); KPIs públicos consolidados: qtd cálculos, contratos, valor contratado, receita/despesa total, qtd notificações abertas/críticas/resolvidas + dias médio resolução, qtd publicações LRF 48-A + hash da última
- [x] **`_marts.yml`** com testes nos PKs + dbt_utils.unique_combination_of_columns ((ano, escola_id, etapa_codigo) e (ano, mes)) + accepted_values (etapa CRECHE..EJA) + dbt_utils.expression_is_true (gap_adequado >= 0) + dbt_utils.accepted_range (ano 2020-2100, mes 1-12)
- [x] **CI job `dbt`** em `.github/workflows/ci.yml` com Postgres 16 service container, aplica V0001..V0011 via psql, instala dbt + dbt_utils, roda `dbt parse` + `dbt compile` + `dbt run` (tests não rodam — sem seed em CI; reservados para staging real)
- [x] **V0011 (cleanup)** — `ALTER COLUMN log_auditoria.acao TYPE VARCHAR(60)`; descoberta pela camada analítica que valores como `"publicar:fundeb_execucao"` (24 chars) e `"executar_lote_manual:admin"` (>10 chars) da Fase 6.C estouravam o VARCHAR(10) original. ALTER não dispara o trigger de proteção da V0006 (cobre só DML)
- [x] **Helm CronJob** `templates/cronjob-dbt-run.yaml` — schedule default `0 */6 * * *`, image custom (`ghcr.io/caqi/dbt-runner` com analytics/ embarcado); RBAC mínimo (read-only role `caqi_analytics`, separado do `caqi` do app); recursos limitados (1 CPU / 1 GB); volumes `tmp/logs/target` em `emptyDir`; values.yaml ganha seção `dbt:` com toggles e secret de senha

Sprint 5.C (defer):
- [ ] **Orquestração avançada** com Dagster (lineage UI + retries automáticos + alertas) — atualmente cronjob simples basta para MVP
- [ ] **Réplica analítica read-only** (logical replication ou snapshot diário) para isolar carga OLTP do dbt em produção — atualmente roda no mesmo banco
- [ ] **Marts adicionais** sob demanda do controle social: `contratos_fornecedor` (concentração de fornecedor), `medicoes_vs_contratos` (saldo executado), `escolas_com_gap_alto` (top-N escolas por pct_gap)
- [ ] **Snapshots dbt** para SCD-2 dos parâmetros de etapa e custos de insumo (rastrear quando subiu o piso salarial, quando reduziu alunos/turma)
- [ ] **dbt seeds CSV estáticos** — códigos IBGE de municípios, dicionário de naturezas PCASP, dicionário de etapas INEP

## Fase 6 — Transparência/LAI

**Status: ⏳ Em andamento.**

Sprint 6.A (concluída) — API pública LAI nos 3 serviços:
- [x] **engine-svc** — GET /api/public/transparencia/{calculos, calculos/{id}, insumos}
- [x] **financeiro-svc** — GET /api/public/transparencia/{contratos, despesas, fundeb-execucao, siope-quadro}
- [x] **compliance-svc** — GET /api/public/transparencia/notificacoes
- [x] CorsConfig em cada svc — `/api/public/**` aceita qualquer origem (allowedOriginPatterns=*), métodos GET+OPTIONS, sem credentials
- [x] SecurityConfig em cada svc — `/api/public/transparencia/**` permitAll
- [x] Cache HTTP `max-age=300, public` em todas as respostas — reduz carga no portal
- [x] LGPD: dados expostos são públicos por natureza (CNPJ PJ, valores agregados, contratos firmados, notificações). NÃO expõe folha individual, CPF, dados pessoais de aluno. Versão pública do SIOPE omite `pendencias` (auditoria interna).

Sprint 6.B (concluída) — Portal público `/transparencia` em Next.js:
- [x] **`lib/api-public.ts`** — fetcher server-only para `/api/public/transparencia/**` (sem injeção de credencial); usa `next: { revalidate: 300 }` para alinhar com o `Cache-Control: max-age=300, public` enviado pelos backends da Fase 6.A; **degradação graciosa**: erros logam mas retornam `null` (página decide como renderizar)
- [x] **middleware.ts** atualizado: matcher exclui `/transparencia`, `/sitemap.xml` e `/robots.txt` da auth NextAuth
- [x] **Route group `(public)`** com layout próprio (header brand + nav + skip-link "Pular para o conteúdo" + footer com bases legais LAI/LRF + link "Acesso administrativo" para `/login`); metadata override `robots: { index: true, follow: true }` (root layout é noindex)
- [x] **6 páginas públicas** (todas Server Components com `dynamic = 'force-dynamic'`):
  - `/transparencia` (landing) — KPIs do exercício corrente: vinculações conforme/atenção, qtd cálculos CAQ, valor global de contratos, qtd notificações abertas; seção "O que esta página publica" com escopo LAI/LGPD
  - `/transparencia/fundeb?ano=N` — 3 gauges MDE/Fundeb70/VAAT15 + receitas/despesas em cards + dl/dd com base legal de cada vinculação; banner amigável quando backend down
  - `/transparencia/contratos` — tabela Lei 14.133 com modalidade/objeto/valor/PNCP + valor agregado; nota sobre LGPD
  - `/transparencia/despesas?ano=N` — 4 KPI cards (total/pessoal/capital/MDE) + nav de anos + tabela competência/natureza/grupo/valor
  - `/transparencia/calculos` + `/calculos/[id]` — lista CAQi/CAQ/gap por escola e detalhe com **2 tabelas de memória** (perfil mínimo/adequado) item-a-item
  - `/transparencia/notificacoes?ano=N` — lista por severidade/status com cores semânticas (info/warn/alta/critica), nav de anos, badges de status
- [x] **SEO**:
  - `app/sitemap.ts` (Next.js convention) — 6 URLs públicas com prioridades 0.7-1.0 e changeFrequency adequados
  - `app/robots.ts` — Allow `/transparencia`, Disallow `/login /dashboard /calculos /contratos /despesas /receitas /fornecedores /simulacoes /notificacoes /fundeb /profile/ /api/ /_next/`
  - `metadata.title.template = '%s · Transparência · CAQ/CAQi'` no layout público
  - `metadata.description` em cada página (Fundeb, contratos, despesas, cálculos, notificações)
- [x] **Acessibilidade WCAG 2.1 AA**:
  - Skip-link com `sr-only focus:not-sr-only` para teclado
  - Landmarks (`<header role>`, `<main id="conteudo" tabIndex={-1}>`, `<nav aria-label>`, `<footer>` implícito no `<footer>`)
  - Heading hierarchy correta (h1 → h2 → h3 sem skip)
  - Tabelas com `<caption className="sr-only">` + `<th scope="col">`
  - `aria-current="page"` nos seletores de ano
  - `<dl><dt><dd>` para definições de termos
  - `role="status"` em mensagens de "sem dados"
- [x] **`tests/e2e/transparencia.spec.ts`** (10 testes):
  - Landing renderiza shell + KPIs com fallback "—" quando BFF down
  - Skip-link recebe foco com Tab
  - `/robots.txt` permite `/transparencia`, bloqueia `/login`+`/dashboard`, lista Sitemap
  - `/sitemap.xml` contém 5 rotas filhas com tags `<urlset>`
  - 5 subpáginas renderizam shell + heading mesmo com backend down
  - Metadata `<meta name="robots">` na landing é `index/follow` (override)
  - SEO: title e description corretos em `/fundeb`
  - 4 axe-core scans (landing + fundeb + contratos + notificacoes) — WCAG 2.1 AA

Sprint 6.C (concluída — parcial 1) — Publicação automática LRF art. 48-A:
- [x] **V0010** estende `publicacao_portal` (V0001) com `referencia VARCHAR(20)` (ano ou ano+mês), `tamanho_bytes BIGINT`, `snapshot JSONB`; **partial unique index** `(tipo, referencia, conteudo_hash) WHERE NOT NULL` (idempotência); índices `(tipo, data_publicacao DESC)` e `(referencia)` para listagens públicas
- [x] **`PublicSnapshotClient`** — cliente HTTP sem auth para `/api/public/transparencia/**` dos serviços engine + financeiro; captura JSON literal (`JsonNode`) — exatamente o que o cidadão vê
- [x] **`PublicacaoService`** transacional, 5 snapshot providers:
  - `fundeb_execucao` (referencia=YYYY) — `/fundeb-execucao?ano=N`
  - `siope_quadro` (referencia=YYYY) — `/siope-quadro?ano=N`
  - `calculos_caq` (referencia=`-`) — `/calculos`
  - `contratos` (referencia=`-`) — `/contratos`
  - `despesas` (referencia=YYYY) — `/despesas?ano=N`
  - **JSON canônico** via `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY` + `ORDER_MAP_ENTRIES_BY_KEYS` → `MessageDigest SHA-256` → hex 64-char
  - **Dedup**: compara hash novo com último publicado para mesmo `(tipo, referencia)` — idêntico = SKIPPED, diferente = INSERT
  - **Audit chain**: cada publicação chama `AuditChainService.registrar("publicacao_portal", id, "publicar:tipo")` + uma entrada de lote ao final
  - Falhas isoladas por snapshot não interrompem os demais (5 tentativas independentes)
- [x] **`PublicacaoScheduler`** — `@Scheduled(cron = "${caqi.publicacao.cron:0 0 */6 * * *}", zone = "America/Sao_Paulo")`; `@ConditionalOnProperty caqi.publicacao.enabled=true` (default true, off em tests)
- [x] **Application class** ganha `@EnableScheduling`
- [x] **Endpoints autenticados** (`PublicacaoController` em `/api/v1/compliance/publicacoes`):
  - `POST /executar?ano=N` (ADMIN) — disparo manual
  - `GET /` (LEITOR) — lista com filtros opcionais `?tipo=` e `?referencia=`
  - `GET /{id}` (LEITOR) — detalhe com snapshot JSON arquivado
- [x] **Endpoints públicos** (extensão de `PublicTransparenciaController`):
  - `GET /api/public/transparencia/publicacoes?tipo=` — trilha pública
  - `GET /api/public/transparencia/publicacoes/{id}` — detalhe + snapshot (cache 5 min)
- [x] **SecurityConfig**: + RBAC `POST /publicacoes/executar` ADMIN
- [x] **PublicacaoServiceTest** (7 cenários Mockito): primeira execução publica todos os 5 + 6 entradas chain (5 + 1 lote); hash idêntico = skip + nada persistido; backend null = falha isolada (2 publicadas + 3 falhas); cliente lança exceção = falha isolada não interrompe; campos persistidos corretos (tipo+ref+hash+tamanho+url+snapshot); `ano=null` usa `Year.now()`; **hash canônico estável** sob reordenação de chaves do JSON
- [x] **Frontend** `/transparencia/publicacoes?tipo=` (Server Component) — tabela cronológica com hash truncado + abbr title (full hash) + tamanho em bytes formatado pt-BR + filtros por tipo com `aria-current=page` + seção "Como verificar a integridade" (passo-a-passo openssl dgst); link no nav do layout público; URL adicionada ao `sitemap.xml`
- [x] **E2E** atualizado: subpáginas inclui `/publicacoes`, sitemap aceita `/transparencia/publicacoes`, novo teste WCAG axe-core + teste de filtro `aria-current` (escopado ao `nav[aria-label="Filtrar por tipo"]` para evitar colisão com `Fundeb / MDE` do layout)

Sprint 6.C (defer):
- [ ] **Repositório documental** — anexos contratuais em MinIO/S3 (CNPJ PJ não-confidencial); metadados públicos via `GET /api/public/transparencia/anexos`; payload estruturado em `contrato.anexos JSONB` (V0011)
- [ ] **K8s CronJob redundante** — `helm/templates/cronjob-publicacao.yaml` que faz `curl POST /publicacoes/executar` com Basic Auth do admin (defesa em profundidade caso `@Scheduled` da JVM caia)
- [ ] **Verificação automatizada de integridade** — endpoint `GET /publicacoes/{id}/verificar` que recalcula o hash do `snapshot` armazenado e compara com `conteudo_hash`

## Fase 7 — Auditoria + Simulador

**Status: ⏳ Em andamento.**

Sprint 7.A (concluída) — Simulador "e se?" no caq-engine-svc:
- [x] **Overrides** record (alunosPorTurmaPorEtapa, qtdPadraoPorInsumo, custoMultiplierPorInsumo) com helper `Overrides.NONE`
- [x] CaqCalculator refatorado: `calcular(req)` delega para `calcular(req, Overrides.NONE)`. Overload aplica overrides em 3 pontos: divisor por_turma (alunos/turma), qtd_padrao do insumo, multiplicador de custo. Backward compat preservada — CaqCalculatorRegressaoTest inalterado
- [x] **SimuladorService**: roda calculator 2x (atual com NONE, simulado com overrides), computa diferenças por (escola, etapa) com deltaCaqi/deltaCaq + pctDeltaCaqi
- [x] DTOs: CenarioSimulacaoDto + DiferencaItemDto + SimulacaoResultadoDto
- [x] Endpoint POST /api/v1/caqi/simulacoes (GESTOR)
- [x] **SimuladorServiceIntegrationTest** (@SpringBootTest + Testcontainers + seed): 2 cenários — alunos/turma 25→20 (deltaCaqi=+850 = +19,14%; deltaCaq=+1300) e PES-001 +25% (mesmo efeito numérico). Confirma que apenas PES-001 muda; MOB-001 e demais inalterados
- [x] SecurityConfig: + RBAC para POST /simulacoes

Já presente desde Fase 3.B (caq-compliance-svc):
- [x] Logs imutáveis com chain SHA-256 sobre log_auditoria + endpoint /verificar (AuditChainService)

Sprint 7.B (concluída) — Painel CACS-Fundeb/CME + presets de simulação:
- [x] **Backend `SimulacaoPreset`** record + **`SimulacaoPresets`** catálogo (3 cenários):
  - **`tempo_integral_universal`** — jornada estendida 8h: PES-001 ×1.50, SER-001 ×1.20, MAN-001 ×1.20, MAT-001 ×1.10; etapas CRECHE/PRE/EF1/EF2/EM; base CF/88 art. 206 IX, PNE meta 6, LDB art. 34 §2°
  - **`menos_5_alunos_turma_ef`** — alunosPorTurma EF1=20, EF2=20 (era 25); sem aumento de despesa, eleva CAQ por aluno em ~25%; base LDB art. 25, PNE meta 7.5, Lei 14.113/2020 art. 12
  - **`reforco_creche_pre`** — 2 docentes/turma em educação infantil (qtdPadrao PES-001 = 2); etapas CRECHE/PRE; base LDB art. 29-31, PNE meta 1, Resolução CNE/CEB 5/2009 art. 8°
- [x] **Endpoints novos** em `SimulacoesController` (caq-engine-svc):
  - `GET /api/v1/caqi/simulacoes/presets` (LEITOR) — lista presets com overrides + base legal
  - `POST /api/v1/caqi/simulacoes/presets/{nome}` (GESTOR) body `{ ano, escolas }` — usa `etapasRecomendadas` do preset, monta `CenarioSimulacaoDto` e roda o simulador existente
- [x] **SecurityConfig**: + RBAC `POST /presets/*` GESTOR (`GET /presets` cai no LEITOR genérico)
- [x] **`SimulacaoPresetsTest`** (6 cenários): nomes únicos / base legal não-vazia / ao menos 1 override por preset; multiplicadores corretos para cada um dos 3; `porNome()` lookup com null/empty; sanity check das referências legais ("PNE meta 6", "LDB art. 25", "Resolução CNE/CEB 5/2009")
- [x] **Frontend** — refeito `/simulacoes` (autenticado):
  - Server Component fetcha presets de `/api/v1/caqi/simulacoes/presets`
  - **`PresetCatalog`** + **`PresetCard`** (clientes) — cada card mostra título/descrição/etapas/impacto esperado/base legal + `<details>` com overrides aplicados + form inline (ano + escolas CSV) + tabela de resultado inline com cores semânticas (delta vermelho/verde)
  - BFF `app/api/simular/presets/[nome]/route.ts` proxy autenticado
  - Mantém atalho "Cenário customizado" → `/simulacoes/nova` (form livre)
- [x] **Frontend público** `/transparencia/conselhos` — **Painel CACS-Fundeb / CME** consolidado:
  - Banner com base legal (Lei 14.113/2020 art. 33-34 e LDB art. 11)
  - 3 gauges Fundeb/MDE/VAAT (reaproveita component da Fase 8)
  - 4 KPIs: notificações abertas (com count de críticas + cor variant), cálculos persistidos, valor contratado, qtd publicações LRF 48-A
  - Lista das 5 alertas mais recentes com border-left por severidade
  - Tabela das 5 últimas publicações com hash + abbr title (full hash)
  - Seção "O que cabe a cada conselho" com `dl/dt/dd` descrevendo atribuições legais de CACS-Fundeb e CME
  - **Tudo degradação graciosa** (`fetchPublic` retorna null se backend down → seções condicionais)
- [x] Layout público: `Conselhos` adicionado ao nav após "Visão geral"
- [x] **Sitemap.ts**: + `/transparencia/conselhos` priority 0.95 changeFrequency daily
- [x] **E2E** (`transparencia.spec.ts`): subpáginas inclui `/conselhos`, novo teste WCAG axe-core, asserção da heading + atribuições legais ("Lei 14.113/2020", "LDB art. 11"); sitemap inclui `/conselhos`

Sprint 7.B (defer):
- [ ] **Cenários comparativos lado-a-lado** — UI para rodar 3+ simulações e visualizar matriz de comparação (tabela cruzada escola/etapa × cenário). Backend já suporta múltiplos POSTs; frontend precisa de design dedicado
- [ ] **Pareceres formais do conselho** — workflow CRUD com draft/assinatura digital (ICP-Brasil opcional) e publicação automática via LRF 48-A; precisa schema novo (V0011 `parecer_conselho`) e fluxo dual de revisão

## Fase 8 — Frontend admin completo

**Status: ⏳ Em andamento.**

Sprint 8.A (concluída) — Auth + BFF + 4 telas operacionais:
- [x] **NextAuth v4** com CredentialsProvider que valida usuário/senha contra Basic Auth do caq-engine-svc; armazena credencial Basic no JWT da sessão (TTL 8h)
- [x] **lib/api-client.ts** (server-only): wrapper `fetchService<T>(svc, path)` que pega Basic da sessão e propaga como header — Server Components e Route Handlers chamam livremente
- [x] **middleware.ts**: protege todas as rotas exceto `/login`, `/api/auth`, `/api/health` e assets — empurra para `/login?callbackUrl=...`
- [x] **app/login/page.tsx** — formulário com tratamento de erro
- [x] **app/(authenticated)/layout.tsx** — header brand + nav + footer com bases legais
- [x] **/dashboard** — KPIs (CAQi médio, gap, notificações abertas), 3 gauges das vinculações (MDE/Fundeb/VAAT) com indicador cumpre/não-cumpre, lista de alertas em aberto. Resiliente a falha parcial.
- [x] **/calculos** — tabela de cálculos persistidos com link para detalhe
- [x] **/calculos/[id]** — memória item-a-item separada por perfil (CAQi mínimo / CAQ adequado)
- [x] **/fundeb** — gauges + agregados de receitas/despesas + glossário
- [x] **/notificacoes** — lista filtrada por status (aberta/em_analise/resolvida/ignorada) com cor por severidade
- [x] Componentes: `Card`, `StatNumber`, `Gauge`, `SignOutButton`
- [x] `lib/types.ts` — types TS dos DTOs Java + helpers fmt.money/percent/date pt-BR
- [x] App Router groups: `(authenticated)` agrupa rotas autenticadas

Sprint 8.B (parcial — concluída):
- [x] **Form de cálculo CAQ** (`/calculos/novo`) — Server Action `criarCalculo` com `useActionState`, valida campos, traduz erros do backend. Botão "+ Novo cálculo" no header da lista.
- [x] **Simulador "e se?" UI** (`/simulacoes` + `/simulacoes/nova`):
  - Página landing com cenários típicos
  - Form client component com inputs para ano, escola, etapa, override de alunos/turma, override de custo (insumo + multiplicador)
  - BFF route handler `/api/simular` proxy para `/api/v1/caqi/simulacoes`
  - Tabela comparativa com CAQi atual/simulado/Δ + CAQ atual/simulado/Δ + Δ%; cores semânticas (vermelho=aumento, verde=redução)
- [x] Componentes form reutilizáveis: `Field`, `Select`, `FormCard`, `ErrorBanner`, `SubmitButton` (com `useFormStatus`)
- [x] Nav atualizado: + Simulações

Sprint 8.B (parcial 2 — concluída): Contratos + medição com preview de retenção
- [x] **/contratos** (lista server) com tabela id/objeto/modalidade/data/valor/PNCP + botão "+ Novo contrato"
- [x] **/contratos/novo** (server + ContratoForm client com useActionState) — Select fornecedor (de `/api/v1/financeiro/fornecedores`), modalidade Lei 14.133 (PREGAO_ELETRONICO/CONCORRENCIA/DISPENSA/INEXIGIBILIDADE/DIALOGO_COMPETITIVO/CONCURSO/LEILAO), validação + erros traduzidos
- [x] **/contratos/[id]** (detalhe) com 3 cards (valor global / total medido / saldo restante) com semantic coloring (saldo<10% → warn, negativo → danger), bloco do fornecedor com badge Simples Nacional, tabela de medições + CTA "+ Registrar medição"
- [x] **/contratos/[id]/medicoes/nova** (client form): registra medição via BFF route `/api/medicoes` → POST `/api/v1/financeiro/contratos/{id}/medicoes`; **mostra preview de retenções inline** (grid 4 col com IRRF/INSS/ISS/PIS/COFINS/CSLL/DAS + total retido + líquido + memória item-a-item com base legal); aviso contextual sobre Simples Nacional vs não-optante
- [x] BFF route `/api/medicoes/route.ts` (single POST) — proxy autenticado
- [x] `lib/types.ts`: + FornecedorDto, MedicaoDto, ItemRetencaoDto, ResultadoRetencaoDto, MedicaoComRetencoesDto + constantes `MODALIDADES_LEI_14133` e `TIPOS_SERVICO_RETENCAO`
- [x] Nav: + "Contratos"

Sprint 8.B (parcial 3 — concluída): CRUD UI completo — fornecedores, receitas, despesas
- [x] **/fornecedores** (lista) + **/fornecedores/novo** (Server Action) com checkbox Optante Simples Nacional + helper texto sobre LC 123/2006 + retenções
- [x] **/receitas** (lista filtrada por ano com agregação por origem em cards) + **/receitas/nova** (Select origem ∈ {impostos, transferencias, Fundeb_VAAF, Fundeb_VAAT, Fundeb_VAAR, outras}, validação de competência YYYYMM)
- [x] **/despesas** (lista filtrada por ano com 4 KPI cards: total, pessoal, capital, MDE) + **/despesas/nova** com **tratamento explícito do bloqueador**:
  - Captura 409 Conflict do backend (EmpenhoBloqueadoException quando flag ativa)
  - Extrai mensagem de Spring ProblemDetail
  - Exibe banner destacado com ícone "⛔ EMPENHO BLOQUEADO PELO VALIDADOR" + motivo + orientação ao usuário
- [x] `lib/types.ts`: + ReceitaDto, DespesaDto, FonteRecursoDto + constantes ORIGENS_RECEITA, SIOPE_GRUPOS
- [x] Nav atualizada com Receitas, Despesas, Fornecedores

Sprint 8.C (concluída) — E2E + WCAG no CI:
- [x] **Playwright 1.48** + **@axe-core/playwright 4.10** adicionados como devDeps
- [x] `playwright.config.ts`: webServer `next start` automático, `baseURL` configurável via `E2E_BASE_URL`, retries=2 em CI, traces/videos retidos em falha, projeto `chromium` único (browsers extras = mais tempo de CI sem ganho de cobertura)
- [x] `tests/e2e/fixtures/axe-helper.ts`: `runAxe(page, testInfo)` com tags `wcag2a/wcag2aa/wcag21a/wcag21aa`; relatório JSON anexado a cada teste; falha imprime regra+nodes
- [x] **`tests/e2e/login.spec.ts`** (7 testes): renderização do form, autocomplete correto (a11y/UX), validação HTML5 (campo `required`), normalização do MFA (uppercase + alphanum), erro genérico em backend down (sem enumeração), axe-core na tela inicial e após erro
- [x] **`tests/e2e/redirect.spec.ts`** (5 testes): GET `/` → /login, `/dashboard` / `/despesas` / `/profile/mfa` → `/login?callbackUrl=...` (preserva), tela de login com callbackUrl é WCAG-compliant
- [x] **CI job `web-e2e`** em `.github/workflows/ci.yml`: depende de `web` (lint/build prévio), instala Playwright com cache de browsers, faz build, roda testes; sobe artifacts `playwright-report` (sempre) e `playwright-traces` (só em falha) por 14 dias
- [x] `.gitignore`: `apps/web/playwright-report/`, `test-results/`, `blob-report/`, `.playwright/`
- [x] `tests/e2e/README.md` documenta escopo (rotas públicas), justifica a delimitação (CI não sobe os 4 microserviços) e ensina como rodar contra staging via `E2E_BASE_URL`

Sprint 8.D (concluída) — E2E telas autenticadas com mock backend:
- [x] **Mock backend** `tests/e2e/mock-backend/server.mjs` — 4 servidores Node.js stdlib em portas 9991-9994 (engine/financeiro/escolar/compliance) servindo fixtures JSON determinísticas. Boot ~100ms vs ~90s do docker-compose Spring; CORS preflight + Cache-Control + Basic Auth opcional (`/api/v1/**` exigem credencial; `/api/public/**` e `/actuator/health` não)
- [x] **Validação Basic Auth** no mock — somente users `admin_test/gestor_test/leitor_test` aceitos (qualquer senha); usuários inválidos recebem 401 + `WWW-Authenticate: Basic realm="caqi"`. Garante que `login.spec.ts` ainda testa a rejeição de credenciais inválidas
- [x] **Playwright config** com **2 webServers em paralelo** (mock + Next.js) e **2 projects** (`public` + `authenticated`); o mock-backend tem health-check em `/api/v1/health` que o Playwright aguarda antes de prosseguir
- [x] **`global-setup.ts`** — antes da suíte autenticada, faz login UI uma vez (admin_test) e salva cookie de sessão NextAuth em `tests/e2e/.auth/admin.json` (gitignored). Specs autenticados carregam o storage state via project config — sem refazer login a cada teste
- [x] **`authenticated.spec.ts`** (14 testes): dashboard renderiza KPIs, /calculos lista, /calculos/1 memória item-a-item (PES-001 + MOB-001), /fundeb mostra 3 gauges com CUMPRE, /simulacoes lista preset "tempo_integral_universal" + apply preset → resultado inline com "+19,14%", /notificacoes mostra VAAT, /despesas + /contratos + /profile/mfa renderizam shell, **logout** clica "Sair" → redirect /login; **4 axe-core scans WCAG 2.1 AA** (dashboard, fundeb, simulacoes, calculos)
- [x] **`.gitignore`** atualizado para `apps/web/tests/e2e/.auth/`
- [x] **README.md** dos testes E2E refeito explicando: 2 projects (public/authenticated), mock backend (vantagens vs docker-compose), global setup, como rodar localmente vs staging real (`E2E_BASE_URL`)
- [x] **CI**: timeout do job `web-e2e` aumentado de 20→25 minutos (mock backend + auth setup adicionam ~30s)

Sprint 8.B (defer):
- [ ] **E2E real-stack opcional** — variante do CI que faz `docker compose up` antes do Playwright para testar contra Spring Boot real, validando contratos de API end-to-end (lento — ~3-5 min — rodar só em PRs com label `e2e-real`)
- [ ] **Visual regression** com Playwright snapshots (Percy alternativo) para detectar regressão de layout em changes de Tailwind/componentes

## Fase 9 — Hardening produção

**Status: ⏳ Em andamento.**

Sprint 9.A (concluída) — Documentação LGPD/operacional + backup CronJob:
- [x] **DPIA/RIPD** (`docs/legal/DPIA_RIPD.md`) — Relatório de Impacto à Proteção de Dados em 10 seções: agentes (controlador=município, operador=Controller, DPO a designar), descrição do tratamento, categorias de dados (incluindo PII de menor de idade no aluno), finalidades+base legal, compartilhamento, retenção (30 anos folha, 5 anos demais), 7 riscos identificados com mitigações já implementadas, direitos do titular, decisões automatizadas (bloqueador empenho, notificações)
- [x] **ROPA** (`docs/legal/ROPA.md`) — Registro de 6 operações de tratamento (matrícula, folha, fornecedor, usuário, log_auditoria, ROPA meta) com base legal+retenção+segurança+direitos por operação
- [x] **Política de Privacidade modelo** (`docs/legal/POLITICA_PRIVACIDADE.md`) — em linguagem clara conforme LGPD art. 9°§1°, pronta para customização pelo município
- [x] **Runbook operacional** (`docs/operacao/RUNBOOK.md`) — 9 sintomas comuns (5xx, deploy travado, postgres down, RabbitMQ congestionado, validador Fundeb, cadeia auditoria quebrada, SIOPE pendências, TLS, incidente LGPD) com diagnóstico+mitigação
- [x] **Backup/Restore** (`docs/operacao/BACKUP_RESTORE.md`) — estratégia em 3 camadas (pg_dump diário, WAL contínuo, snapshot), RPO/RTO alvo, procedimento step-by-step de restauração, drill mensal obrigatório
- [x] **Hardening checklist** (`docs/operacao/HARDENING_PRODUCAO.md`) — 12 seções de pré-condições para go-live: segredos, RBAC, rede, BD, auditoria, observabilidade, compliance, deploy, DR, docs, pen-test, validação funcional + aprovações finais
- [x] **CronJob backup Helm** (`charts/caqi/templates/cronjob-backup.yaml`): pg_dump comprimido + upload S3 com KMS encryption; configurável via `values.yaml/backup.*`; emptyDir tmp 5Gi; rodando como user 1000 read-only-rootfs; concurrencyPolicy=Forbid
- [x] `values.yaml` ganha seção `backup` com schedule, retention, destination (s3/minio), resources

Sprint 9.B (parcial — concluída):
- [x] **V0006 — Trigger de proteção em log_auditoria**: REVOKE UPDATE/DELETE/TRUNCATE do role `caqi` + `block_log_auditoria_changes()` raise exception 42501; trigger `BEFORE UPDATE OR DELETE OR TRUNCATE` FOR EACH STATEMENT. INSERT/SELECT preservados. Procedimento de exceção documentado em HARDENING_PRODUCAO.md §5.1 (DISABLE TRIGGER + ATA + RIPD)
- [x] **ProtecaoLogAuditoriaTest** (@SpringBootTest + Testcontainers): valida que INSERT funciona, UPDATE/DELETE/TRUNCATE são bloqueados com mensagem "append-only"; testa o procedimento de exceção (DISABLE → operação → ENABLE)
- [x] HARDENING_PRODUCAO.md atualizado: marca trigger como done com comando do procedimento

Sprint 9.B (parcial 2 — concluída): MFA TOTP backend
- [x] **V0007 — usuario_mfa table** (username PK, secret_base32, enabled, created_at, enabled_at, ultima_validacao)
- [x] JPA UsuarioMfa + UsuarioMfaRepository
- [x] Lib `dev.samstevens.totp:totp:1.7.1` adicionada via version catalog
- [x] **MfaService** (RFC 6238, SHA1, 6 dígitos, 30s, ±1 janela): setup() gera secret 160-bit base32 + URI otpauth (issuer=CAQi-{municipioNome}); enable(codigo) valida primeiro código e ativa; verify(codigo) usado em fluxo de login futuro; disable(codigo) exige confirmação; estaHabilitado() para checks
- [x] **MfaController**: POST /api/v1/auth/mfa/{setup,enable,verify,disable} + GET /status (autenticado, qualquer role)
- [x] SecurityConfig: + `/api/v1/auth/mfa/**` autenticado (cada usuário gerencia o próprio)
- [x] MfaServiceTest unit (Mockito + repo em memória) cobre 5 cenários: setup gera URI correta, enable valida código (correto/incorreto), verify só funciona quando enabled, disable exige confirmação, estaHabilitado reflete ciclo

Sprint 9.B (parcial 3 — concluída): Integração MFA no NextAuth + UI `/profile/mfa`
- [x] `lib/auth-options.ts`: CredentialsProvider ganha campo `mfaCode`. Authorize: 1) valida basic auth; 2) consulta `/api/v1/auth/mfa/status` (fail-closed se /status indisponível); 3) se enabled, exige `mfaCode` válido via `POST /verify`; 4) sessão criada normalmente em sucesso
- [x] `/login` ganha campo "Código MFA (se habilitado)" — opcional, `inputMode=numeric pattern=\\d{6} autoComplete=one-time-code`
- [x] `/profile/mfa` (server) consulta `/status` e delega para `MfaManagementUI` (client) que tem 3 modos: idle (com status) / configurando (QR + secret + campo código) / desativando (campo código)
- [x] **QR via `qrcode.react@4.0.1`** — adicionado em apps/web/package.json
- [x] BFF route `/api/mfa/[acao]/route.ts` — proxy whitelist (setup/enable/verify/disable/status), GET só /status, POST nos demais; injeta Basic Auth da sessão via `fetchService`
- [x] Nav ganha link "MFA"
- [x] `lib/types.ts`: + `MfaStatusDto` + `MfaSetupResponseDto`

Sprint 9.B (parcial 4 — concluída): Backup codes para recuperação de MFA
- [x] **V0008** — colunas `backup_codes_hashes JSONB` e `backup_codes_generated_at` em `usuario_mfa`
- [x] UsuarioMfa entity ganha `backupCodesHashes` (List<String> via @JdbcTypeCode JSONB)
- [x] **MfaService**:
  - `enable()` agora retorna `EnableResultado(success, backupCodes)` — em sucesso, gera 8 códigos one-time-use de 10 chars (alfabeto sem 0/O/1/I, ≈50 bits entropia cada), persiste como SHA-256 hex, devolve raw UMA vez
  - `verify(codigo)` aceita TOTP (6 dígitos) OU backup code (10 alphanum); normaliza espaços/hífens/case; backup code é consumido (removido do array)
  - `regenerateBackupCodes(username, codigoTotp)` invalida conjunto antigo, exige TOTP válido
  - `countBackupCodesRemaining(username)` para o badge de status
- [x] DTOs: novos `MfaVerifyDto`, `MfaEnableResponseDto`, `MfaBackupCodesResponseDto`; `MfaStatusDto` ganha `backupCodesRemaining`
- [x] MfaController: `enable()` devolve backup codes; novo `POST /regenerate-backup-codes`; `/verify` usa MfaVerifyDto
- [x] Frontend `MfaManagementUI` com modo `mostrando_codes`: exibe 8 códigos pós-enable/regenerate em grid 4×2 com "Copiar todos" e "Baixar .txt"; status mostra `X/8` com alerta vermelho se < 3
- [x] `/login` campo MFA aceita 6-11 chars alphanum (TOTP ou backup code); auth-options regex relaxa para `\\d{6}|[A-Z2-9]{10}`
- [x] BFF whitelist `/api/mfa/[acao]` ganha `regenerate-backup-codes`
- [x] `lib/types.ts` atualizado
- [x] MfaServiceTest cobre 9 cenários (setup vazio em backup, enable gera 8 únicos, TOTP, backup one-time-use, normalização, rejeita formato inválido, regenerate, disable, ciclo)

Sprint 9.C (concluída) — mTLS Istio + checklist pen-test + scan:
- [x] **`charts/caqi/templates/istio-peerauthentication.yaml`** — `PeerAuthentication` STRICT em todo o namespace (pods sem sidecar não conseguem aceitar conexões dos serviços CAQi)
- [x] **`charts/caqi/templates/istio-destinationrule.yaml`** — `DestinationRule` `ISTIO_MUTUAL` para tráfego intra-mesh (`*.<ns>.svc.cluster.local`)
- [x] **`values.yaml`** ganha bloco `istio: { enabled: false, mtlsMode: STRICT }` (default off para não quebrar clusters sem Istio)
- [x] **`docs/operacao/MTLS_ISTIO.md`** — guia de migração de PERMISSIVE → STRICT, troubleshooting, custo computacional (~250m CPU + 400Mi RAM extras), tráfego para fora do mesh (ServiceEntry para Postgres/RabbitMQ/S3), defer AuthorizationPolicy granular para Sprint 9.D
- [x] **`docs/operacao/PEN_TEST_CHECKLIST.md`** — roteiro OWASP Top 10 (2021) com casos específicos do domínio: IDOR em calculos, bypass RBAC, path traversal upload Censo, bypass bloqueador empenho, salt pseudonimização Censo, JWT NextAuth, cadeia auditoria, headers segurança; controles LGPD/LRF/Lei 14.133; cadência (quadrimestral interno + anual externo); pré-requisitos (autorização formal escrita Lei 12.737/2012); ferramentas recomendadas + reportagem (achado >=MEDIUM ao DPO + ata CACS-Fundeb)
- [x] **`scripts/security-scan.sh`** — wrapper local com 7 seções: gitleaks, semgrep OWASP, OWASP Dependency-Check, npm audit, trivy config, kubescape K8s, checks customizados (.env, senhas literais, CPF em arquivos não-test). Skip silencioso se ferramenta não instalada; exit 1 em achado HIGH/CRITICAL
- [x] **CI job `security`** em `.github/workflows/ci.yml` — gitleaks-action (varre histórico) + trivy-action `scan-type: config` (Dockerfiles + Helm + IaC) com severity HIGH/CRITICAL; warn-only no MVP (`exit-code: 0`), subir para 1 em prod

Sprint 9.B (restante — defer):
- [ ] Auth Gov.br OAuth2 — NextAuth provider customizado + Spring Resource Server validando JWT (precisa credenciais Gov.br reais para testar)
- [ ] Pen-test externo anual com consultoria sob NDA
- [ ] AuthorizationPolicy Istio granular (Sprint 9.D) — engine só aceita do web/compliance, financeiro só do web/engine/compliance, escolar só do web /import; defer princípio do menor privilégio em rede

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
