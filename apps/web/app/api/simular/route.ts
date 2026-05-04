import { NextResponse } from 'next/server';

import { ApiError, fetchService } from '@/lib/api-client';

interface CenarioBody {
  ano: number;
  escolas: string[];
  etapas: string[];
  alunosPorTurma?: Record<string, number>;
  qtdPadraoInsumos?: Record<string, number>;
  custoMultiplierInsumos?: Record<string, number>;
}

/** BFF: proxy para o caq-engine-svc /api/v1/caqi/simulacoes com Basic Auth da sessão. */
export async function POST(req: Request) {
  const body = (await req.json()) as CenarioBody;
  try {
    const result = await fetchService<unknown>('engine', '/api/v1/caqi/simulacoes', {
      method: 'POST',
      body: JSON.stringify(body),
    });
    return NextResponse.json(result);
  } catch (e) {
    const status = e instanceof ApiError ? e.status : 500;
    const message = e instanceof Error ? e.message : 'Erro desconhecido';
    return NextResponse.json({ erro: message }, { status });
  }
}
