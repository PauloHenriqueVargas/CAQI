{{ config(materialized='view') }}

select
    log_id,
    usuario_id,
    tabela,
    registro_id,
    acao,                  -- insert | update | delete | publicar:* | executar_lote_*
    carimbo_tempo,
    hash_antes,
    hash_depois
from {{ source('caqi', 'log_auditoria') }}
