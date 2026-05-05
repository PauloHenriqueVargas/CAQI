{{ config(materialized='view') }}

select
    custo_id,
    insumo_id,
    perfil,                       -- minimo | adequado (V0003)
    custo_unitario,
    fonte_preco,
    indice_atualizacao,
    vigencia_inicio,
    vigencia_fim
from {{ source('caqi', 'custo_insumo') }}
