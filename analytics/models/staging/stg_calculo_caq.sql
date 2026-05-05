{{ config(materialized='view') }}

-- Resultado consolidado do cálculo CAQ/CAQi por (escola, etapa, ano).
-- A coluna física é `gap_execucao` (V0001); na camada analítica usamos
-- `gap_adequado` (mais alinhado ao domínio: distância CAQi → CAQ).
select
    calculo_id,
    etapa_id,
    escola_id,
    ano,
    valor_caqi_aluno_ano,
    valor_caq_aluno_ano,
    gap_execucao as gap_adequado
from {{ source('caqi', 'calculo_caq') }}
