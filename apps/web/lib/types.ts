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
