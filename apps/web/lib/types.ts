// Types TypeScript que espelham os DTOs Java do backend.
// Campos numéricos chegam como string (BigDecimal serializado) — preservar precisão.
// Em Fase 9, gerar via openapi-typescript a partir dos /v3/api-docs dos serviços.

export type Money = string; // "1234.56" — não usar number, perde precisão

export interface ItemResultadoDto {
  escolaId: string;
  etapaId: string;
  caqiAlunoAno: Money;
  caqAlunoAno: Money;
  gapAlunoAno: Money;
}

export interface CalculoCaqResumoDto {
  id: number;
  escolaId: number;
  etapaId: number;
  ano: number;
  valorCaqiAlunoAno: Money;
  valorCaqAlunoAno: Money;
  gapAdequado: Money;
}

export interface CalculoCaqItemDto {
  id: number;
  perfil: 'minimo' | 'adequado';
  insumoCodigo: string;
  insumoNome: string;
  tipoAplicacao: 'por_aluno' | 'por_turma' | 'por_escola';
  qtdAplicada: Money;
  custoUnitario: Money;
  custoAnual: Money;
  divisor: Money;
  custoAlunoAno: Money;
  baseCalculo: string;
}

export interface CalculoCaqDetalheDto extends CalculoCaqResumoDto {
  itens: CalculoCaqItemDto[];
}

export interface ExecucaoFundebDto {
  ano: number;
  receitasImpostos: Money;
  receitasFundeb: Money;
  receitasVaat: Money;
  despesasMde: Money;
  despesasPessoalFundeb: Money;
  despesasCapitalVaat: Money;
  pctMde: Money;
  pctFundebPessoal: Money;
  pctVaatCapital: Money;
  cumpreMde: boolean;
  cumpreFundebPessoal: boolean;
  cumpreVaatCapital: boolean;
}

export interface NotificacaoDto {
  id: number;
  tipo: string;
  severidade: 'info' | 'warn' | 'alta' | 'critica';
  titulo: string;
  descricao: string;
  anoReferencia: number;
  baseLegal: string | null;
  payload: Record<string, unknown> | null;
  status: 'aberta' | 'em_analise' | 'resolvida' | 'ignorada';
  createdAt: string;
  resolvedAt: string | null;
}

export interface ContratoDto {
  id: number;
  fornecedorId: number;
  objeto: string;
  dataAssinatura: string;
  valorGlobal: Money;
  modalidade: string;
  pncpId: string | null;
}

export interface MfaStatusDto {
  enabled: boolean;
  username: string;
  backupCodesRemaining: number;
}

export interface MfaSetupResponseDto {
  secret: string;
  otpauthUri: string;
}

export interface MfaEnableResponseDto {
  enabled: boolean;
  username: string;
  /** 8 backup codes one-time-use — só retornados aqui. Exibir e exigir que o usuário guarde. */
  backupCodes: string[];
}

export interface MfaBackupCodesResponseDto {
  backupCodes: string[];
}

export interface FornecedorDto {
  id: number;
  cnpj: string;
  nome: string;
  optanteSimples: boolean;
  municipio: string | null;
}

export interface MedicaoDto {
  id: number;
  contratoId: number;
  competencia: string;
  valorMedido: Money;
  notaFiscal: string | null;
}

export interface ItemRetencaoDto {
  tributo: string;
  aliquota: Money;
  base: Money;
  valor: Money;
  baseLegal: string | null;
  observacao: string | null;
}

export interface ResultadoRetencaoDto {
  valorBruto: Money;
  irrf: Money;
  inss: Money;
  iss: Money;
  pis: Money;
  cofins: Money;
  csll: Money;
  das: Money;
  totalRetido: Money;
  valorLiquido: Money;
  memoria: ItemRetencaoDto[];
}

export interface MedicaoComRetencoesDto {
  medicao: MedicaoDto;
  retencoesPreview: ResultadoRetencaoDto | null;
}

export const MODALIDADES_LEI_14133 = [
  'PREGAO_ELETRONICO',
  'CONCORRENCIA',
  'DISPENSA',
  'INEXIGIBILIDADE',
  'DIALOGO_COMPETITIVO',
  'CONCURSO',
  'LEILAO',
] as const;

export const TIPOS_SERVICO_RETENCAO = [
  'GERAL',
  'LIMPEZA_CONSERVACAO',
  'ENGENHARIA',
  'VIGILANCIA',
  'TRANSPORTE_CARGAS',
  'MANUTENCAO_PREDIAL',
  'OBRAS_CIVIS',
] as const;

export const ORIGENS_RECEITA = [
  'impostos',
  'transferencias',
  'Fundeb_VAAF',
  'Fundeb_VAAT',
  'Fundeb_VAAR',
  'outras',
] as const;

export const SIOPE_GRUPOS = ['MDE', 'Geral'] as const;

export interface ReceitaDto {
  id: number;
  competencia: string;
  valor: Money;
  origem: string;
  pcasp: string | null;
}

export interface DespesaDto {
  id: number;
  competencia: string;
  natureza: string;
  valor: Money;
  fonteRecursoId: number | null;
  pcasp: string | null;
  siopeGrupo: string | null;
  contratoId: number | null;
  isPessoal: boolean;
  isCapital: boolean;
}

export interface FonteRecursoDto {
  id: number;
  tipo: 'Propria' | 'VAAF' | 'VAAT' | 'VAAR' | 'Outras';
  descricao: string | null;
}

/** Presets de simulação CACS-Fundeb/CME. */
export interface SimulacaoPresetDto {
  nome: string;
  titulo: string;
  descricao: string;
  impactoEsperado: string;
  baseLegal: string[];
  alunosPorTurma: Record<string, Money>;
  qtdPadraoInsumos: Record<string, Money>;
  custoMultiplierInsumos: Record<string, Money>;
  etapasRecomendadas: string[];
}

export interface DiferencaItemSimulacaoDto {
  escolaId: string;
  etapaCodigo: string;
  caqiAtual: Money;
  caqiSimulado: Money;
  deltaCaqi: Money;
  caqAtual: Money;
  caqSimulado: Money;
  deltaCaq: Money;
  pctDeltaCaqi: Money;
}

export interface SimulacaoResultadoDto {
  ano: number;
  diferencas: DiferencaItemSimulacaoDto[];
}

/** Trilha de publicações LRF art. 48-A. */
export interface PublicacaoDto {
  id: number;
  tipo: string;          // fundeb_execucao | siope_quadro | calculos_caq | contratos | despesas
  referencia: string;    // ano (YYYY) ou "-"
  conteudoHash: string;  // SHA-256 hex
  tamanhoBytes: number;
  urlPublica: string | null;
  dataPublicacao: string;
  snapshot: unknown | null; // só vem no detalhe
}

/** Helpers de formatação BR */
export const fmt = {
  money: (v: Money | string | number | null | undefined): string => {
    if (v == null) return '-';
    const num = typeof v === 'number' ? v : parseFloat(v);
    if (Number.isNaN(num)) return '-';
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  },
  percent: (v: Money | string | number | null | undefined): string => {
    if (v == null) return '-';
    const num = typeof v === 'number' ? v : parseFloat(v);
    if (Number.isNaN(num)) return '-';
    return num.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '%';
  },
  date: (iso: string | null | undefined): string => {
    if (!iso) return '-';
    return new Date(iso).toLocaleDateString('pt-BR');
  },
};
