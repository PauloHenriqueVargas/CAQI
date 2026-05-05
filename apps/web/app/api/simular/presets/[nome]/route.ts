import { NextRequest, NextResponse } from 'next/server';

import { ApiError, fetchService } from '@/lib/api-client';

interface PresetExecRequest {
  ano: number;
  escolas: string[];
}

/**
 * BFF: aplica preset de simulação. Proxy autenticado para
 * caq-engine-svc /api/v1/caqi/simulacoes/presets/{nome}.
 */
export async function POST(req: NextRequest, { params }: { params: { nome: string } }) {
  const body = (await req.json()) as PresetExecRequest;
  const nome = encodeURIComponent(params.nome);
  try {
    const result = await fetchService<unknown>(
      'engine',
      `/api/v1/caqi/simulacoes/presets/${nome}`,
      { method: 'POST', body: JSON.stringify(body) },
    );
    return NextResponse.json(result);
  } catch (e) {
    const status = e instanceof ApiError ? e.status : 500;
    const message = e instanceof Error ? e.message : 'Erro desconhecido';
    return NextResponse.json({ erro: message }, { status });
  }
}
