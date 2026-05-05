{{ config(materialized='view') }}

-- Agregação Censo INEP: contagem de matrículas por (escola, etapa, ano).
-- Diferente da tabela operacional `matricula` (per-aluno) — esta vem dos
-- microdados oficiais e alimenta análises de cobertura/atendimento.
select
    resumo_id,
    importacao_id,
    ano_censo,
    escola_inep,
    escola_id,
    etapa_codigo,
    qtd_alunos
from {{ source('caqi', 'censo_matricula_resumo') }}
