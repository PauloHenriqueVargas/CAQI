{{ config(materialized='view') }}

-- 1:1 com escola, com nomes levemente normalizados.
select
    escola_id,
    nome             as escola_nome,
    inep_id          as escola_inep,
    rede,
    localizacao,
    coalesce(situacao, 'ativa') as situacao,
    created_at,
    updated_at
from {{ source('caqi', 'escola') }}
