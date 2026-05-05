{{ config(materialized='view') }}

-- Despesas por competência, natureza PCASP e fonte de recurso.
-- Adicionamos flags derivadas de uso comum (is_pessoal, is_capital)
-- para evitar repetir o LIKE em cada modelo a jusante.
select
    despesa_id,
    competencia,
    cast(substring(competencia, 1, 4) as integer) as ano,
    cast(substring(competencia, 5, 2) as integer) as mes,
    to_date(competencia, 'YYYYMM')                 as competencia_date,
    natureza,
    valor,
    fonte_recurso_id,
    pcasp,
    siope_grupo,
    contrato_id,

    -- Convenção PCASP: 3.1.x = pessoal, 4.x = capital, 3.3.x = consumo/serviços
    case when natureza like '3.1%' then true else false end as is_pessoal,
    case when natureza like '4%'   then true else false end as is_capital
from {{ source('caqi', 'despesa') }}
