-- SEED — APENAS DEV/TEST.
--
-- Reproduz literal a Planilha_Base_CAQi_Preenchida.xlsx em
-- docs/references/. Existência condicionada à inclusão de classpath:db/seed
-- nas locations do Flyway (configurada em application-dev.yml e
-- application-test.yml). NUNCA carrega em prod.
--
-- Conteúdo:
--   - 6 etapas (CRECHE, PRE, EF1, EF2, EJA, ESP)
--   - parametros_etapa (alunos/turma, jornada, etc.)
--   - 9 insumos com qtd_padrao + custo vigente
--   - 3 escolas (Escola Municipal A, B, C)
--   - 1.250 alunos seed + matriculas:
--       Escola A: 200 Pré + 400 EF1 = 600
--       Escola B: 120 Creche + 350 EF2 = 470
--       Escola C: 180 EJA = 180

BEGIN;

-- ─────────────────────────── ETAPAS ───────────────────────────
INSERT INTO etapa (codigo, modalidade, descricao) VALUES
  ('CRECHE', 'Padrao',  'Creche')                  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO etapa (codigo, modalidade, descricao) VALUES
  ('PRE',    'Regular', 'Pré-escola')              ON CONFLICT (codigo) DO NOTHING;
INSERT INTO etapa (codigo, modalidade, descricao) VALUES
  ('EF1',    'Regular', 'EF Anos Iniciais')        ON CONFLICT (codigo) DO NOTHING;
INSERT INTO etapa (codigo, modalidade, descricao) VALUES
  ('EF2',    'Regular', 'EF Anos Finais')          ON CONFLICT (codigo) DO NOTHING;
INSERT INTO etapa (codigo, modalidade, descricao) VALUES
  ('EJA',    'Regular', 'EJA')                     ON CONFLICT (codigo) DO NOTHING;
INSERT INTO etapa (codigo, modalidade, descricao) VALUES
  ('ESP',    'SRM',     'Educação Especial — SRM') ON CONFLICT (codigo) DO NOTHING;

-- ───────────────── PARÂMETROS POR ETAPA ─────────────────
INSERT INTO parametro_etapa (etapa_id, tempo, alunos_por_turma, carga_horaria_docente_h_sem, jornada_dias_ano, coef_rural, vigencia_inicio)
SELECT etapa_id, 'integral', 12, 40, 200, 1.0, DATE '2025-01-01' FROM etapa WHERE codigo='CRECHE';
INSERT INTO parametro_etapa (etapa_id, tempo, alunos_por_turma, carga_horaria_docente_h_sem, jornada_dias_ano, coef_rural, vigencia_inicio)
SELECT etapa_id, 'parcial',  15, 30, 200, 1.0, DATE '2025-01-01' FROM etapa WHERE codigo='CRECHE';
INSERT INTO parametro_etapa (etapa_id, tempo, alunos_por_turma, carga_horaria_docente_h_sem, jornada_dias_ano, coef_rural, vigencia_inicio)
SELECT etapa_id, 'parcial',  25, 20, 200, 1.0, DATE '2025-01-01' FROM etapa WHERE codigo='PRE';
INSERT INTO parametro_etapa (etapa_id, tempo, alunos_por_turma, carga_horaria_docente_h_sem, jornada_dias_ano, coef_rural, vigencia_inicio)
SELECT etapa_id, 'parcial',  25, 20, 200, 1.0, DATE '2025-01-01' FROM etapa WHERE codigo='EF1';
INSERT INTO parametro_etapa (etapa_id, tempo, alunos_por_turma, carga_horaria_docente_h_sem, jornada_dias_ano, coef_rural, vigencia_inicio)
SELECT etapa_id, 'parcial',  30, 20, 200, 1.0, DATE '2025-01-01' FROM etapa WHERE codigo='EF2';
INSERT INTO parametro_etapa (etapa_id, tempo, alunos_por_turma, carga_horaria_docente_h_sem, jornada_dias_ano, coef_rural, vigencia_inicio)
SELECT etapa_id, 'noturno',  25, 16, 200, 1.0, DATE '2025-01-01' FROM etapa WHERE codigo='EJA';
INSERT INTO parametro_etapa (etapa_id, tempo, alunos_por_turma, carga_horaria_docente_h_sem, jornada_dias_ano, coef_rural, vigencia_inicio)
SELECT etapa_id, 'parcial',  10, 16, 200, 1.0, DATE '2025-01-01' FROM etapa WHERE codigo='ESP';

-- ─────────────────────── INSUMOS ───────────────────────
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('INF-001', 'Sala de aula: manutenção anual',     'Infraestrutura', 'por_escola', 'serviço',     'Todas',  1)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('EQP-001', 'Computador (laboratório)',           'Equipamentos',   'por_escola', 'un',          'EF/EM',  15)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('MOB-001', 'Carteira escolar',                   'Mobiliário',     'por_aluno',  'un',          'Todas',  1)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('MAT-001', 'Kit material didático aluno',        'Materiais',      'por_aluno',  'kit',         'Todas',  1)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('SER-001', 'Limpeza e conservação',              'Serviços',       'por_escola', 'mês',         'Todas',  12)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('PES-001', 'Professor — remuneração anual (40h)','Pessoal',        'por_turma',  'docente',     'Todas',  1)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('PES-002', 'Coordenador Pedagógico — anual',     'Pessoal',        'por_escola', 'coordenador', 'Todas',  1)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('MAN-001', 'Manutenção predial preventiva',      'Manutenção',     'por_escola', 'ano',         'Todas',  1)
  ON CONFLICT (codigo) DO NOTHING;
INSERT INTO insumo (codigo, nome, categoria, tipo_aplicacao, unidade, etapa_aplicavel, qtd_padrao) VALUES
  ('TEC-001', 'Internet banda larga',               'Serviços',       'por_escola', 'mês',         'Todas',  12)
  ON CONFLICT (codigo) DO NOTHING;

-- ─────────────────────── CUSTOS VIGENTES ───────────────────────
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id, 15000, 'Contrato',        'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='INF-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id,  3500, 'SINAPI/Contrato', 'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='EQP-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id,   280, 'SINAPI',          'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='MOB-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id,   350, 'Pregão',          'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='MAT-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id,  5000, 'Contrato',        'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='SER-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id, 85000, 'Folha',           'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='PES-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id, 95000, 'Folha',           'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='PES-002';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id, 20000, 'Contrato',        'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='MAN-001';
INSERT INTO custo_insumo (insumo_id, custo_unitario, fonte_preco, indice_atualizacao, vigencia_inicio)
SELECT insumo_id,   300, 'Contrato',        'IPCA', DATE '2025-01-01' FROM insumo WHERE codigo='TEC-001';

-- ─────────────────────── ESCOLAS ───────────────────────
INSERT INTO escola (nome, inep_id, rede, localizacao, situacao) VALUES
  ('Escola Municipal A', 'INEP-A', 'municipal', 'urbana', 'ativa'),
  ('Escola Municipal B', 'INEP-B', 'municipal', 'urbana', 'ativa'),
  ('Escola Municipal C', 'INEP-C', 'municipal', 'urbana', 'ativa')
  ON CONFLICT DO NOTHING;

-- ─────────────────────── ALUNOS + MATRÍCULAS ───────────────────────
-- Cria 1.250 alunos e distribui:
--   1..200      → Escola A, PRE
--   201..600    → Escola A, EF1
--   601..720    → Escola B, CRECHE
--   721..1070   → Escola B, EF2
--   1071..1250  → Escola C, EJA
INSERT INTO aluno (nome, localizacao)
SELECT 'Aluno seed ' || gs, 'urbana' FROM generate_series(1, 1250) gs;

DO $$
DECLARE
  esc_a BIGINT := (SELECT escola_id FROM escola WHERE nome = 'Escola Municipal A');
  esc_b BIGINT := (SELECT escola_id FROM escola WHERE nome = 'Escola Municipal B');
  esc_c BIGINT := (SELECT escola_id FROM escola WHERE nome = 'Escola Municipal C');
  et_pre BIGINT := (SELECT etapa_id FROM etapa WHERE codigo = 'PRE');
  et_ef1 BIGINT := (SELECT etapa_id FROM etapa WHERE codigo = 'EF1');
  et_cre BIGINT := (SELECT etapa_id FROM etapa WHERE codigo = 'CRECHE');
  et_ef2 BIGINT := (SELECT etapa_id FROM etapa WHERE codigo = 'EF2');
  et_eja BIGINT := (SELECT etapa_id FROM etapa WHERE codigo = 'EJA');
  base_aluno BIGINT := (SELECT MIN(aluno_id) FROM aluno WHERE nome LIKE 'Aluno seed %');
BEGIN
  INSERT INTO matricula (aluno_id, escola_id, etapa_id, situacao, data_inicio)
  SELECT base_aluno + gs - 1, esc_a, et_pre, 'ativa', DATE '2025-02-01' FROM generate_series(1, 200) gs;

  INSERT INTO matricula (aluno_id, escola_id, etapa_id, situacao, data_inicio)
  SELECT base_aluno + gs - 1, esc_a, et_ef1, 'ativa', DATE '2025-02-01' FROM generate_series(201, 600) gs;

  INSERT INTO matricula (aluno_id, escola_id, etapa_id, situacao, data_inicio)
  SELECT base_aluno + gs - 1, esc_b, et_cre, 'ativa', DATE '2025-02-01' FROM generate_series(601, 720) gs;

  INSERT INTO matricula (aluno_id, escola_id, etapa_id, situacao, data_inicio)
  SELECT base_aluno + gs - 1, esc_b, et_ef2, 'ativa', DATE '2025-02-01' FROM generate_series(721, 1070) gs;

  INSERT INTO matricula (aluno_id, escola_id, etapa_id, situacao, data_inicio)
  SELECT base_aluno + gs - 1, esc_c, et_eja, 'ativa', DATE '2025-02-01' FROM generate_series(1071, 1250) gs;
END $$;

-- ─────────────────────── ÍNDICES (IPCA + SINAPI) ───────────────────────
INSERT INTO indice_preco (nome, competencia, valor) VALUES
  ('IPCA',         '2025-01', 0.45), ('SINAPI_Obras', '2025-01', 0.80),
  ('IPCA',         '2025-02', 0.38), ('SINAPI_Obras', '2025-02', 0.75),
  ('IPCA',         '2025-03', 0.62), ('SINAPI_Obras', '2025-03', 0.60),
  ('IPCA',         '2025-04', 0.46), ('SINAPI_Obras', '2025-04', 0.55),
  ('IPCA',         '2025-05', 0.30), ('SINAPI_Obras', '2025-05', 0.50),
  ('IPCA',         '2025-06', 0.28), ('SINAPI_Obras', '2025-06', 0.48),
  ('IPCA',         '2025-07', 0.35), ('SINAPI_Obras', '2025-07', 0.52),
  ('IPCA',         '2025-08', 0.40), ('SINAPI_Obras', '2025-08', 0.49),
  ('IPCA',         '2025-09', 0.36), ('SINAPI_Obras', '2025-09', 0.47),
  ('IPCA',         '2025-10', 0.42), ('SINAPI_Obras', '2025-10', 0.46),
  ('IPCA',         '2025-11', 0.41), ('SINAPI_Obras', '2025-11', 0.45),
  ('IPCA',         '2025-12', 0.44), ('SINAPI_Obras', '2025-12', 0.50)
  ON CONFLICT (nome, competencia) DO NOTHING;

COMMIT;
