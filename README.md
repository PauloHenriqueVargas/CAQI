# Sistema CAQ/CAQi

Sistema SaaS para gestão do **Custo Aluno Qualidade (CAQ/CAQi)** em redes municipais e estaduais de educação básica, com conformidade nativa às exigências legais brasileiras (CF/88, LDB, PNE, Fundeb, LRF, LAI, SIAFIC, Lei 14.133/2021, LGPD).

## Estado atual

Repositório recém-criado. Já incorpora artefatos de Sprint 0 produzidos previamente:

| Artefato | Local | Descrição |
|---|---|---|
| OpenAPI 3.1 (esqueleto) | [openapi/caqi-openapi.yaml](openapi/caqi-openapi.yaml) | Endpoints v0.1.0 — caqi, fundeb, siope, escolar, compras, transparência, auditoria, tributário |
| Modelo de dados (DDL) | [apps/api/migrations/0001_initial_schema.sql](apps/api/migrations/0001_initial_schema.sql) | 26 tabelas PostgreSQL — escolas, etapas, insumos, Fundeb, contratos, retenções, auditoria, ROPA |
| Coleção Postman | [openapi/postman/](openapi/postman/) | Collection + tests + environment alinhados ao OpenAPI |
| Prompt-mestre original | [docs/references/PROMPT_MESTRE_Sistema_CAQ_CAQi.pdf](docs/references/PROMPT_MESTRE_Sistema_CAQ_CAQi.pdf) | Especificação de origem (resumo do CAQ.pdf) |
| Anexo CAQ (conceito + módulos) | [docs/references/PROMPT_MESTRE_anexo.pdf](docs/references/PROMPT_MESTRE_anexo.pdf) | Páginas 6–8, 20–29, 34 — base conceitual |
| Sprint 0 — stories + modelo | [docs/references/Sprint0_CAQ_Stories_ModeloDados.docx](docs/references/Sprint0_CAQ_Stories_ModeloDados.docx) | User stories e modelo de dados v0 |
| Planilha base CAQi | [docs/references/Planilha_Base_CAQi_Preenchida.xlsx](docs/references/Planilha_Base_CAQi_Preenchida.xlsx) | Catálogo inicial de insumos e parâmetros |
| Checklist de conformidade | [docs/legal/Checklist_Conformidade_CAQ.xlsx](docs/legal/Checklist_Conformidade_CAQ.xlsx) | Mapa CF/LDB/PNE/Fundeb/LRF/LAI/SIAFIC/14.133/LGPD |
| Governança do projeto | [docs/Governanca_Projeto_CAQ.xlsx](docs/Governanca_Projeto_CAQ.xlsx) | Estrutura de governança |

## Próximos passos (a validar)

Veja [docs/STACK.md](docs/STACK.md) (proposta de stack) e [docs/ROADMAP.md](docs/ROADMAP.md) (faseamento da entrega).

## Estrutura

```
CAQI/
├── apps/
│   ├── api/          # Backend FastAPI (a implementar)
│   └── web/          # Frontend React (a implementar)
├── analytics/        # dbt — modelos analíticos e motor CAQ (a implementar)
├── openapi/          # Spec OpenAPI 3.1 + Postman
├── docs/             # Documentação, referências, governança, conformidade
├── infra/            # Docker, CI/CD, IaC
└── .github/          # Workflows e templates
```

## Licença

A definir.
