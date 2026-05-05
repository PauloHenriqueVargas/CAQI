{{ config(materialized='view') }}

select
    etapa_id,
    codigo as etapa_codigo,
    modalidade,
    descricao
from {{ source('caqi', 'etapa') }}
