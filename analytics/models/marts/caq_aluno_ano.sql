{{ config(
    materialized='table',
    indexes=[
        {'columns': ['ano', 'escola_id', 'etapa_codigo'], 'unique': true},
        {'columns': ['ano']},
        {'columns': ['etapa_codigo']}
    ]
) }}

-- Mart final: CAQi/CAQ/gap por (escola, etapa, ano), com nomes
-- desnormalizados (escola_nome, etapa_codigo) e estatísticas de itens.
-- BI consume aqui — não vá direto às tabelas operacionais.
--
-- Grain: 1 linha por (calculo_id) = (escola_id, etapa_id, ano).
-- A tabela operacional `calculo_caq` impõe UNIQUE em (etapa_id, escola_id, ano).
with calc as (
    select * from {{ ref('stg_calculo_caq') }}
),
escola as (
    select escola_id, escola_nome, escola_inep, rede, localizacao
    from {{ ref('stg_escola') }}
),
etapa as (
    select etapa_id, etapa_codigo, modalidade, descricao
    from {{ ref('stg_etapa') }}
),
itens_pivot as (
    select * from {{ ref('int_calculo_perfil_pivoted') }}
),
agg_itens as (
    select
        calculo_id,
        count(*)                                        as num_insumos,
        sum(case when custo_aluno_ano_minimo   is not null then 1 else 0 end) as num_itens_minimo,
        sum(case when custo_aluno_ano_adequado is not null then 1 else 0 end) as num_itens_adequado,
        sum(coalesce(case when insumo_codigo like 'PES%' then custo_aluno_ano_minimo   end, 0)) as pessoal_aluno_ano_minimo,
        sum(coalesce(case when insumo_codigo like 'PES%' then custo_aluno_ano_adequado end, 0)) as pessoal_aluno_ano_adequado
    from itens_pivot
    group by 1
)
select
    c.calculo_id,
    c.ano,
    c.escola_id,
    e.escola_nome,
    e.escola_inep,
    e.rede,
    e.localizacao,
    c.etapa_id,
    et.etapa_codigo,
    et.modalidade           as etapa_modalidade,
    et.descricao            as etapa_descricao,

    c.valor_caqi_aluno_ano,
    c.valor_caq_aluno_ano,
    c.gap_adequado,
    case when c.valor_caqi_aluno_ano > 0
         then round(c.gap_adequado / c.valor_caqi_aluno_ano * 100, 2)
         else null
    end as pct_gap,

    coalesce(a.num_insumos, 0)              as num_insumos,
    coalesce(a.num_itens_minimo, 0)         as num_itens_minimo,
    coalesce(a.num_itens_adequado, 0)       as num_itens_adequado,
    coalesce(a.pessoal_aluno_ano_minimo, 0)   as pessoal_aluno_ano_minimo,
    coalesce(a.pessoal_aluno_ano_adequado, 0) as pessoal_aluno_ano_adequado,

    -- Share do pessoal no CAQi (sanity check: a maior parte do CAQ é pessoal)
    case when c.valor_caqi_aluno_ano > 0
         then round(a.pessoal_aluno_ano_minimo / c.valor_caqi_aluno_ano * 100, 2)
         else null
    end as pct_pessoal_no_caqi
from calc c
inner join escola e   on c.escola_id = e.escola_id
inner join etapa  et  on c.etapa_id  = et.etapa_id
left  join agg_itens a on a.calculo_id = c.calculo_id
