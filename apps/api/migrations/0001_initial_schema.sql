-- Modelo de Dados CAQ/CAQi — PostgreSQL DDL
-- Gerações: public schema; ajuste conforme necessário.
BEGIN;
SET client_encoding = 'UTF8';

CREATE TABLE IF NOT EXISTS escola (
  escola_id BIGSERIAL PRIMARY KEY,
  nome TEXT NOT NULL,
  inep_id VARCHAR(20),
  rede VARCHAR(20) DEFAULT 'municipal',
  localizacao VARCHAR(10) CHECK (localizacao IN ('urbana','rural')),
  endereco TEXT,
  situacao VARCHAR(10) DEFAULT 'ativa',
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
CREATE TABLE IF NOT EXISTS etapa (
  etapa_id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(30) UNIQUE NOT NULL, -- ex.: CRECHE, PRE, EF1, EF2, EJA, ESP
  modalidade VARCHAR(30),
  descricao TEXT
);
CREATE TABLE IF NOT EXISTS parametro_etapa (
  parametro_id BIGSERIAL PRIMARY KEY,
  etapa_id BIGINT REFERENCES etapa(etapa_id) ON DELETE RESTRICT,
  tempo VARCHAR(20) CHECK (tempo IN ('parcial','integral','noturno')),
  alunos_por_turma NUMERIC(6,2),
  carga_horaria_docente_h_sem NUMERIC(6,2),
  jornada_dias_ano NUMERIC(6,2),
  coef_rural NUMERIC(6,3) DEFAULT 1.000,
  vigencia_inicio DATE NOT NULL,
  vigencia_fim DATE
);
CREATE TABLE IF NOT EXISTS indice_preco (
  indice_id BIGSERIAL PRIMARY KEY,
  nome VARCHAR(50) NOT NULL, -- IPCA, SINAPI_Obras, etc.
  competencia CHAR(7) NOT NULL, -- YYYY-MM
  valor NUMERIC(10,4) NOT NULL, -- percentual ou índice
  UNIQUE(nome, competencia)
);
CREATE TABLE IF NOT EXISTS insumo (
  insumo_id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(30) UNIQUE NOT NULL,
  nome TEXT NOT NULL,
  categoria VARCHAR(30) NOT NULL, -- Infraestrutura/Equipamentos/Serviços/Pessoal/Manutenção/Materiais
  tipo_aplicacao VARCHAR(15) NOT NULL CHECK (tipo_aplicacao IN ('por_aluno','por_turma','por_escola')),
  unidade VARCHAR(20) NOT NULL,
  etapa_aplicavel VARCHAR(100) DEFAULT 'Todas'
);
CREATE TABLE IF NOT EXISTS custo_insumo (
  custo_id BIGSERIAL PRIMARY KEY,
  insumo_id BIGINT REFERENCES insumo(insumo_id) ON DELETE CASCADE,
  custo_unitario NUMERIC(14,2) NOT NULL,
  fonte_preco VARCHAR(50),
  indice_atualizacao VARCHAR(50),
  vigencia_inicio DATE NOT NULL,
  vigencia_fim DATE
);
CREATE TABLE IF NOT EXISTS turma (
  turma_id BIGSERIAL PRIMARY KEY,
  escola_id BIGINT REFERENCES escola(escola_id),
  etapa_id BIGINT REFERENCES etapa(etapa_id),
  modalidade VARCHAR(30),
  qtd_alunos INTEGER CHECK (qtd_alunos >= 0),
  created_at TIMESTAMP DEFAULT now()
);
CREATE TABLE IF NOT EXISTS aluno (
  aluno_id BIGSERIAL PRIMARY KEY,
  nome TEXT NOT NULL,
  data_nasc DATE,
  responsavel_nome TEXT,
  localizacao VARCHAR(10) CHECK (localizacao IN ('urbana','rural')),
  created_at TIMESTAMP DEFAULT now()
);
CREATE TABLE IF NOT EXISTS matricula (
  matricula_id BIGSERIAL PRIMARY KEY,
  aluno_id BIGINT REFERENCES aluno(aluno_id) ON DELETE CASCADE,
  escola_id BIGINT REFERENCES escola(escola_id) ON DELETE RESTRICT,
  etapa_id BIGINT REFERENCES etapa(etapa_id) ON DELETE RESTRICT,
  situacao VARCHAR(15) DEFAULT 'ativa',
  data_inicio DATE NOT NULL,
  data_fim DATE
);
CREATE TABLE IF NOT EXISTS pessoa_servidor (
  servidor_id BIGSERIAL PRIMARY KEY,
  nome TEXT NOT NULL,
  cpf_hash CHAR(64), -- armazenar hash, não o CPF em claro
  cargo VARCHAR(60), -- docente/tecnico
  lotacao_escola_id BIGINT REFERENCES escola(escola_id),
  carga_horaria NUMERIC(5,2), -- horas semanais
  created_at TIMESTAMP DEFAULT now()
);
CREATE TABLE IF NOT EXISTS fonte_recurso (
  fonte_recurso_id BIGSERIAL PRIMARY KEY,
  tipo VARCHAR(30) NOT NULL, -- Propria/VAAF/VAAT/VAAR/Outras
  descricao TEXT
);
CREATE TABLE IF NOT EXISTS folha_evento (
  evento_id BIGSERIAL PRIMARY KEY,
  servidor_id BIGINT REFERENCES pessoa_servidor(servidor_id),
  competencia CHAR(6) NOT NULL, -- YYYYMM
  rubrica VARCHAR(60) NOT NULL,
  valor NUMERIC(14,2) NOT NULL,
  fonte_recurso_id BIGINT REFERENCES fonte_recurso(fonte_recurso_id),
  classificacao_fundeb BOOLEAN DEFAULT FALSE -- conta para 70%?
);
CREATE TABLE IF NOT EXISTS receita (
  receita_id BIGSERIAL PRIMARY KEY,
  competencia CHAR(6) NOT NULL, -- YYYYMM
  valor NUMERIC(14,2) NOT NULL,
  origem VARCHAR(50), -- impostos/transferencias
  pcasp VARCHAR(20)
);
CREATE TABLE IF NOT EXISTS fornecedor (
  fornecedor_id BIGSERIAL PRIMARY KEY,
  cnpj VARCHAR(18) UNIQUE,
  nome TEXT,
  optante_simples BOOLEAN DEFAULT FALSE,
  municipio VARCHAR(60)
);
CREATE TABLE IF NOT EXISTS contrato (
  contrato_id BIGSERIAL PRIMARY KEY,
  fornecedor_id BIGINT REFERENCES fornecedor(fornecedor_id),
  objeto TEXT NOT NULL,
  data_assinatura DATE NOT NULL,
  valor_global NUMERIC(14,2),
  modalidade VARCHAR(30),
  pncp_id VARCHAR(60)
);
CREATE TABLE IF NOT EXISTS medicao_contrato (
  medicao_id BIGSERIAL PRIMARY KEY,
  contrato_id BIGINT REFERENCES contrato(contrato_id) ON DELETE CASCADE,
  competencia CHAR(6) NOT NULL,
  valor_medido NUMERIC(14,2) NOT NULL,
  nota_fiscal VARCHAR(40)
);
CREATE TABLE IF NOT EXISTS despesa (
  despesa_id BIGSERIAL PRIMARY KEY,
  competencia CHAR(6) NOT NULL,
  natureza VARCHAR(20) NOT NULL, -- ex.: 3.3.90.30
  valor NUMERIC(14,2) NOT NULL,
  fonte_recurso_id BIGINT REFERENCES fonte_recurso(fonte_recurso_id),
  pcasp VARCHAR(20),
  siope_grupo VARCHAR(60),
  contrato_id BIGINT REFERENCES contrato(contrato_id)
);
CREATE TABLE IF NOT EXISTS retencao_tributaria (
  retencao_id BIGSERIAL PRIMARY KEY,
  despesa_id BIGINT REFERENCES despesa(despesa_id) ON DELETE CASCADE,
  tipo VARCHAR(10) NOT NULL, -- IRRF/INSS/ISS/PIS/COFINS/CSLL/DAS
  base_calculo NUMERIC(14,2) NOT NULL,
  valor_retido NUMERIC(14,2) NOT NULL,
  documento_arrecadacao VARCHAR(40)
);
CREATE TABLE IF NOT EXISTS calculo_caq (
  calculo_id BIGSERIAL PRIMARY KEY,
  etapa_id BIGINT REFERENCES etapa(etapa_id),
  escola_id BIGINT REFERENCES escola(escola_id),
  ano INTEGER NOT NULL,
  valor_caqi_aluno_ano NUMERIC(14,2),
  valor_caq_aluno_ano NUMERIC(14,2),
  gap_execucao NUMERIC(14,2),
  UNIQUE (etapa_id, escola_id, ano)
);
CREATE TABLE IF NOT EXISTS evento_siope (
  evento_siope_id BIGSERIAL PRIMARY KEY,
  competencia CHAR(6) NOT NULL,
  tipo VARCHAR(20) NOT NULL, -- envio/retorno/pendencia
  situacao VARCHAR(10) NOT NULL, -- ok/erro
  descricao TEXT
);
CREATE TABLE IF NOT EXISTS publicacao_portal (
  publicacao_id BIGSERIAL PRIMARY KEY,
  escola_id BIGINT REFERENCES escola(escola_id),
  tipo VARCHAR(40) NOT NULL, -- CAQ/ExecucaoFundeb/MDE/Contrato
  conteudo_hash CHAR(64),
  url_publica TEXT,
  data_publicacao TIMESTAMP DEFAULT now()
);
CREATE TABLE IF NOT EXISTS usuario (
  usuario_id BIGSERIAL PRIMARY KEY,
  nome TEXT NOT NULL,
  perfil VARCHAR(40) NOT NULL, -- Administrador/Contador/Diretor/DPO/etc.
  email TEXT,
  ativo BOOLEAN DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS log_auditoria (
  log_id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT REFERENCES usuario(usuario_id),
  tabela TEXT NOT NULL,
  registro_id TEXT NOT NULL,
  acao VARCHAR(10) NOT NULL, -- insert/update/delete
  carimbo_tempo TIMESTAMP DEFAULT now(),
  hash_antes CHAR(64),
  hash_depois CHAR(64)
);
CREATE TABLE IF NOT EXISTS ropa_registro (
  ropa_id BIGSERIAL PRIMARY KEY,
  tabela TEXT NOT NULL,
  base_legal TEXT NOT NULL, -- execução de política pública/consentimento/etc.
  controlador TEXT NOT NULL,
  operador TEXT,
  prazo_retencao INTEGER -- meses
);
COMMIT;