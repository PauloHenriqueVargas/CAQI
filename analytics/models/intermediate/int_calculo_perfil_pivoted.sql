{{ config(materialized='view') }}

-- Pivota itens do cálculo por perfil (minimo / adequado) para facilitar a
-- comparação CAQi vs CAQ. Uma linha por (calculo_id, insumo_codigo).
with itens as (
    select * from {{ ref('stg_calculo_caq_item') }}
)
select
    calculo_id,
    insumo_codigo,
    max(insumo_nome)   as insumo_nome,
    max(tipo_aplicacao) as tipo_aplicacao,
    -- Perfil mínimo (CAQi)
    sum(case when perfil = 'minimo'   then qtd_aplicada    end) as qtd_minimo,
    sum(case when perfil = 'minimo'   then custo_unitario  end) as custo_unitario_minimo,
    sum(case when perfil = 'minimo'   then custo_anual     end) as custo_anual_minimo,
    sum(case when perfil = 'minimo'   then custo_aluno_ano end) as custo_aluno_ano_minimo,
    -- Perfil adequado (CAQ)
    sum(case when perfil = 'adequado' then qtd_aplicada    end) as qtd_adequado,
    sum(case when perfil = 'adequado' then custo_unitario  end) as custo_unitario_adequado,
    sum(case when perfil = 'adequado' then custo_anual     end) as custo_anual_adequado,
    sum(case when perfil = 'adequado' then custo_aluno_ano end) as custo_aluno_ano_adequado
from itens
group by 1, 2
