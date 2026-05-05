{{ config(materialized='view') }}

-- Soma de receitas por (ano, origem). Espelha a agregação que o
-- FundebService.calcular() faz na borda OLTP, mas aqui materializada para
-- consultas analíticas e agregações cross-ano.
--
-- Categorias derivadas:
--  - eh_mde_base    = origem ∈ {impostos, transferencias}    → base do MDE 25%
--  - eh_fundeb_base = origem ∈ {Fundeb_VAAF, Fundeb_VAAT, Fundeb_VAAR} → base Fundeb 70%
--  - eh_vaat        = origem = Fundeb_VAAT                   → base VAAT 15%
select
    ano,
    origem,
    sum(valor) as valor_total,
    count(*)   as qtd_lancamentos,

    case when origem in ('impostos', 'transferencias') then true else false end as eh_mde_base,
    case when origem in ('Fundeb_VAAF', 'Fundeb_VAAT', 'Fundeb_VAAR') then true else false end as eh_fundeb_base,
    case when origem = 'Fundeb_VAAT' then true else false end as eh_vaat
from {{ ref('stg_receita') }}
group by 1, 2
