{{ config(materialized='view') }}

select
    insumo_id,
    codigo as insumo_codigo,
    nome as insumo_nome,
    categoria,
    tipo_aplicacao,
    unidade,
    etapa_aplicavel,
    qtd_padrao
from {{ source('caqi', 'insumo') }}
