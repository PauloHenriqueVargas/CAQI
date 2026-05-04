import { NextResponse } from 'next/server';

import { ApiError, fetchService } from '@/lib/api-client';

interface MedicaoBody {
  contratoId: number;
  competencia: string;
  valorMedido: string | number;
  notaFiscal?: string | null;
  tipoServico?: string | null;
  aliquotaIssMunicipal?: string | number | null;
}

/**
 * BFF: registra medição contratual via POST /financeiro/contratos/{id}/medicoes,
 * incluindo preview opcional de retenção quando tipoServico e aliquotaIssMunicipal
 * são informados. Devolve MedicaoComRetencoesDto.
 */
export async function POST(req: Request) {
  const body = (await req.json()) as MedicaoBody;
  const { contratoId, ...payload } = body;
  if (!contratoId) {
    return NextResponse.json({ erro: 'contratoId é obrigatório' }, { status: 400 });
  }
  try {
    const result = await fetchService<unknown>(
      'financeiro',
      `/api/v1/financeiro/contratos/${contratoId}/medicoes`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    );
    return NextResponse.json(result);
  } catch (e) {
    const status = e instanceof ApiError ? e.status : 500;
    const msg = e instanceof Error ? e.message : 'Erro desconhecido';
    return NextResponse.json({ erro: msg }, { status });
  }
}
