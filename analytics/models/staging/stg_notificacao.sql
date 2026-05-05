{{ config(materialized='view') }}

select
    notificacao_id,
    tipo,
    severidade,                  -- info | warn | alta | critica
    titulo,
    descricao,
    ano_referencia,
    base_legal,
    payload_json,
    status,                      -- aberta | em_analise | resolvida | ignorada
    created_at,
    resolved_at,
    case when resolved_at is not null
         then extract(epoch from (resolved_at - created_at)) / 86400.0
         else null
    end as dias_para_resolver
from {{ source('caqi', 'notificacao') }}
