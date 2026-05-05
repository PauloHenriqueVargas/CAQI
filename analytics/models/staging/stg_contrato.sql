{{ config(materialized='view') }}

select
    contrato_id,
    fornecedor_id,
    objeto,
    data_assinatura,
    valor_global,
    modalidade,             -- Lei 14.133/2021: PREGAO_ELETRONICO, CONCORRENCIA, etc.
    pncp_id,
    extract(year from data_assinatura)::int as ano_assinatura
from {{ source('caqi', 'contrato') }}
