{{ config(materialized='view') }}

-- Despesas com classificação Fundeb/MDE pré-aplicada via JOIN com fonte_recurso.
-- Categorias para os marts:
--   classe_pessoal_fundeb = is_pessoal AND fonte é Fundeb (VAAF/VAAT/VAAR)  → 70% pessoal
--   classe_capital_vaat   = is_capital AND fonte = VAAT                     → 15% capital
--   eh_mde                = siope_grupo = 'MDE'                             → 25% MDE
select
    d.despesa_id,
    d.competencia,
    d.ano,
    d.mes,
    d.competencia_date,
    d.natureza,
    d.valor,
    d.fonte_recurso_id,
    fr.tipo as fonte_tipo,
    d.pcasp,
    d.siope_grupo,
    d.contrato_id,
    d.is_pessoal,
    d.is_capital,

    case when d.is_pessoal and fr.tipo in ('VAAF', 'VAAT', 'VAAR')
         then true else false end as classe_pessoal_fundeb,
    case when d.is_capital and fr.tipo = 'VAAT'
         then true else false end as classe_capital_vaat,
    case when d.siope_grupo = 'MDE' then true else false end as eh_mde
from {{ ref('stg_despesa') }} d
left join {{ ref('stg_fonte_recurso') }} fr
       on d.fonte_recurso_id = fr.fonte_recurso_id
