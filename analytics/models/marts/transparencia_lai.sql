{{ config(
    materialized='table',
    indexes=[
        {'columns': ['ano'], 'unique': true}
    ]
) }}

-- Mart: KPIs do portal público (LAI/LRF) por ano. Usado para dashboards
-- de transparência ativa e para auditoria do controle social — uma
-- tabela única que responde "o que é público neste exercício?".
--
-- Grain: 1 linha por ano com lançamentos.

with anos as (
    select distinct ano from {{ ref('stg_receita') }}
    union
    select distinct ano from {{ ref('stg_despesa') }}
    union
    select distinct ano_referencia as ano from {{ ref('stg_notificacao') }}
    union
    select distinct ano from {{ ref('stg_calculo_caq') }}
),

calculos as (
    select ano, count(*) as qtd_calculos
    from {{ ref('stg_calculo_caq') }}
    group by 1
),

contratos as (
    select
        ano_assinatura as ano,
        count(*)               as qtd_contratos,
        sum(coalesce(valor_global, 0)) as valor_contratado
    from {{ ref('stg_contrato') }}
    group by 1
),

despesas_anuais as (
    select
        ano,
        sum(valor) as despesa_total,
        sum(case when eh_mde                then valor end) as despesa_mde,
        sum(case when classe_pessoal_fundeb then valor end) as despesa_pessoal_fundeb,
        sum(case when classe_capital_vaat   then valor end) as despesa_capital_vaat
    from {{ ref('int_despesas_classificadas') }}
    group by 1
),

receitas_anuais as (
    select
        ano,
        sum(valor) as receita_total
    from {{ ref('stg_receita') }}
    group by 1
),

notificacoes as (
    select
        ano_referencia as ano,
        count(*)                                                       as qtd_notificacoes,
        sum(case when status = 'aberta'   then 1 else 0 end)           as qtd_notif_abertas,
        sum(case when severidade = 'critica' then 1 else 0 end)        as qtd_notif_criticas,
        sum(case when status = 'resolvida' then 1 else 0 end)          as qtd_notif_resolvidas,
        avg(dias_para_resolver)                                        as dias_medio_resolucao
    from {{ ref('stg_notificacao') }}
    group by 1
),

publicacoes as (
    -- referencia pode ser '-' (atemporal) ou YYYY (anual). Para o mart
    -- por ano, contamos apenas as anuais; as atemporais entram em "global".
    select
        cast(referencia as integer) as ano,
        count(*)               as qtd_publicacoes,
        max(data_publicacao)   as ultima_publicacao,
        max(case when row_n = 1 then conteudo_hash end) as hash_ultima_publicacao
    from (
        select
            *,
            row_number() over (partition by referencia order by data_publicacao desc) as row_n
        from {{ ref('stg_publicacao_portal') }}
        where referencia ~ '^[0-9]{4}$'
    ) p
    group by 1
)

select
    a.ano,
    coalesce(c.qtd_calculos,           0) as qtd_calculos_publicados,
    coalesce(co.qtd_contratos,         0) as qtd_contratos,
    coalesce(co.valor_contratado,      0) as valor_contratado_total,

    coalesce(r.receita_total,          0) as receita_total,
    coalesce(d.despesa_total,          0) as despesa_total,
    coalesce(d.despesa_mde,             0) as despesa_mde,
    coalesce(d.despesa_pessoal_fundeb,  0) as despesa_pessoal_fundeb,
    coalesce(d.despesa_capital_vaat,    0) as despesa_capital_vaat,

    coalesce(n.qtd_notificacoes,       0) as qtd_notificacoes,
    coalesce(n.qtd_notif_abertas,      0) as qtd_notif_abertas,
    coalesce(n.qtd_notif_criticas,     0) as qtd_notif_criticas,
    coalesce(n.qtd_notif_resolvidas,   0) as qtd_notif_resolvidas,
    n.dias_medio_resolucao,

    coalesce(p.qtd_publicacoes,        0) as qtd_publicacoes_lrf48a,
    p.ultima_publicacao,
    p.hash_ultima_publicacao,

    current_timestamp                  as gerado_em
from anos a
left join calculos        c   on a.ano = c.ano
left join contratos       co  on a.ano = co.ano
left join despesas_anuais d   on a.ano = d.ano
left join receitas_anuais r   on a.ano = r.ano
left join notificacoes    n   on a.ano = n.ano
left join publicacoes     p   on a.ano = p.ano
order by a.ano desc
