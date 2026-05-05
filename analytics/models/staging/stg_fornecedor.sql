{{ config(materialized='view') }}

select
    fornecedor_id,
    cnpj,
    nome as fornecedor_nome,
    optante_simples,
    municipio
from {{ source('caqi', 'fornecedor') }}
