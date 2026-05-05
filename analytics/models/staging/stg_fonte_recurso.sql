{{ config(materialized='view') }}

select
    fonte_recurso_id,
    tipo,                   -- Propria | VAAF | VAAT | VAAR | Outras
    descricao
from {{ source('caqi', 'fonte_recurso') }}
