{{ config(materialized='view') }}

select
    item_id,
    calculo_id,
    perfil,                  -- minimo | adequado (V0004)
    insumo_id,
    insumo_codigo,
    insumo_nome,
    tipo_aplicacao,          -- por_aluno | por_turma | por_escola
    qtd_aplicada,
    custo_unitario,
    custo_anual,
    divisor,
    custo_aluno_ano,
    base_calculo,
    created_at
from {{ source('caqi', 'calculo_caq_item') }}
