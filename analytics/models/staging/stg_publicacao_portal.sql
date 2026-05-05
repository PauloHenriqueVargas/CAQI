{{ config(materialized='view') }}

-- Trilha de publicações automáticas LRF art. 48-A (V0010).
-- O snapshot JSONB completo é exposto ao público via API; aqui só
-- expomos metadados para análise (dedup, frequência, lacunas).
select
    publicacao_id,
    tipo,                                   -- fundeb_execucao | siope_quadro | calculos_caq | contratos | despesas
    referencia,                             -- ano YYYY ou '-'
    conteudo_hash,
    tamanho_bytes,
    url_publica,
    data_publicacao,
    extract(year from data_publicacao)::int as ano_publicacao,
    extract(month from data_publicacao)::int as mes_publicacao
from {{ source('caqi', 'publicacao_portal') }}
