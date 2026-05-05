{{ config(
    materialized='table',
    indexes=[
        {'columns': ['ano', 'mes'], 'unique': true},
        {'columns': ['ano']}
    ]
) }}

-- Mart: execução Fundeb/MDE/VAAT mês a mês com totais ACUMULADOS no ano.
-- Permite ao CACS/CME ver a trajetória de cumprimento — não apenas o
-- número final do ano.
--
-- Grain: 1 linha por (ano, mes). Mesmo meses sem lançamento ainda aparecem
-- (gerados via CROSS JOIN com calendário).

with calendario as (
    select
        ano,
        mes
    from (
        select distinct ano from {{ ref('stg_receita') }}
        union
        select distinct ano from {{ ref('stg_despesa') }}
    ) anos
    cross join (
        select generate_series(1, 12) as mes
    ) meses
),

receitas_mensal as (
    select
        ano,
        mes,
        sum(case when origem in ('impostos', 'transferencias')         then valor end) as r_mde_mes,
        sum(case when origem in ('Fundeb_VAAF', 'Fundeb_VAAT', 'Fundeb_VAAR') then valor end) as r_fundeb_mes,
        sum(case when origem = 'Fundeb_VAAT'                            then valor end) as r_vaat_mes
    from {{ ref('stg_receita') }}
    group by 1, 2
),

despesas_mensal as (
    select
        ano,
        mes,
        sum(case when eh_mde                  then valor end) as d_mde_mes,
        sum(case when classe_pessoal_fundeb   then valor end) as d_pessoal_fundeb_mes,
        sum(case when classe_capital_vaat     then valor end) as d_capital_vaat_mes
    from {{ ref('int_despesas_classificadas') }}
    group by 1, 2
),

base as (
    select
        c.ano,
        c.mes,
        coalesce(r.r_mde_mes,             0) as r_mde_mes,
        coalesce(r.r_fundeb_mes,          0) as r_fundeb_mes,
        coalesce(r.r_vaat_mes,            0) as r_vaat_mes,
        coalesce(d.d_mde_mes,             0) as d_mde_mes,
        coalesce(d.d_pessoal_fundeb_mes,  0) as d_pessoal_fundeb_mes,
        coalesce(d.d_capital_vaat_mes,    0) as d_capital_vaat_mes
    from calendario c
    left join receitas_mensal r using (ano, mes)
    left join despesas_mensal d using (ano, mes)
),

acumulado as (
    select
        ano,
        mes,
        r_mde_mes,
        r_fundeb_mes,
        r_vaat_mes,
        d_mde_mes,
        d_pessoal_fundeb_mes,
        d_capital_vaat_mes,
        sum(r_mde_mes)             over win as r_mde_acum,
        sum(r_fundeb_mes)          over win as r_fundeb_acum,
        sum(r_vaat_mes)            over win as r_vaat_acum,
        sum(d_mde_mes)             over win as d_mde_acum,
        sum(d_pessoal_fundeb_mes)  over win as d_pessoal_fundeb_acum,
        sum(d_capital_vaat_mes)    over win as d_capital_vaat_acum
    from base
    window win as (partition by ano order by mes rows between unbounded preceding and current row)
)

select
    ano,
    mes,
    -- Lançamentos do mês
    r_mde_mes,
    r_fundeb_mes,
    r_vaat_mes,
    d_mde_mes,
    d_pessoal_fundeb_mes,
    d_capital_vaat_mes,
    -- Acumulado no ano
    r_mde_acum,
    r_fundeb_acum,
    r_vaat_acum,
    d_mde_acum,
    d_pessoal_fundeb_acum,
    d_capital_vaat_acum,
    -- Percentuais (vinculações)
    case when r_mde_acum    > 0 then round(d_mde_acum / r_mde_acum * 100, 2)                   else null end as pct_mde_acum,
    case when r_fundeb_acum > 0 then round(d_pessoal_fundeb_acum / r_fundeb_acum * 100, 2)      else null end as pct_fundeb_pessoal_acum,
    case when r_vaat_acum   > 0 then round(d_capital_vaat_acum / r_vaat_acum * 100, 2)          else null end as pct_vaat_capital_acum,
    -- Booleans cumpre
    case when r_mde_acum    > 0 and (d_mde_acum / r_mde_acum) >= 0.25 then true else false end as cumpre_mde,
    case when r_fundeb_acum > 0 and (d_pessoal_fundeb_acum / r_fundeb_acum) >= 0.70 then true else false end as cumpre_fundeb_pessoal,
    case when r_vaat_acum   > 0 and (d_capital_vaat_acum / r_vaat_acum) >= 0.15 then true else false end as cumpre_vaat_capital
from acumulado
order by ano, mes
