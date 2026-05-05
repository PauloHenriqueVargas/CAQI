{{ config(materialized='view') }}

select
    medicao_id,
    contrato_id,
    competencia,
    cast(substring(competencia, 1, 4) as integer) as ano,
    cast(substring(competencia, 5, 2) as integer) as mes,
    valor_medido,
    nota_fiscal
from {{ source('caqi', 'medicao_contrato') }}
