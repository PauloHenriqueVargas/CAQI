{{ config(materialized='view') }}

-- Receitas por competência (CHAR(6) YYYYMM) e origem.
-- Convertemos a competência em ano/mes/competencia_date para facilitar
-- agregações temporais downstream.
select
    receita_id,
    competencia,
    cast(substring(competencia, 1, 4) as integer)  as ano,
    cast(substring(competencia, 5, 2) as integer)  as mes,
    to_date(competencia, 'YYYYMM')                  as competencia_date,
    valor,
    origem,
    pcasp
from {{ source('caqi', 'receita') }}
