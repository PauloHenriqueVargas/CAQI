import { NextResponse } from 'next/server';

import { ApiError, fetchService } from '@/lib/api-client';

const ACOES_PERMITIDAS = new Set([
  'setup', 'enable', 'verify', 'disable', 'status',
  'regenerate-backup-codes',
]);

/**
 * BFF proxy genérico para /api/v1/auth/mfa/{acao} no caq-engine-svc.
 * Whitelist de ações por segurança. fetchService injeta Basic Auth da sessão.
 */

export async function GET(_req: Request, { params }: { params: { acao: string } }) {
  if (!ACOES_PERMITIDAS.has(params.acao)) {
    return NextResponse.json({ erro: 'ação inválida' }, { status: 404 });
  }
  if (params.acao !== 'status') {
    return NextResponse.json({ erro: 'método não permitido' }, { status: 405 });
  }
  try {
    const result = await fetchService<unknown>('engine', `/api/v1/auth/mfa/${params.acao}`);
    return NextResponse.json(result);
  } catch (e) {
    return errorResp(e);
  }
}

export async function POST(req: Request, { params }: { params: { acao: string } }) {
  if (!ACOES_PERMITIDAS.has(params.acao) || params.acao === 'status') {
    return NextResponse.json({ erro: 'ação inválida' }, { status: 404 });
  }
  let body: unknown = undefined;
  if (req.headers.get('content-length')) {
    try { body = await req.json(); } catch { body = undefined; }
  }
  try {
    const result = await fetchService<unknown>('engine', `/api/v1/auth/mfa/${params.acao}`, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    });
    return NextResponse.json(result);
  } catch (e) {
    return errorResp(e);
  }
}

function errorResp(e: unknown) {
  const status = e instanceof ApiError ? e.status : 500;
  const message = e instanceof Error ? e.message : 'Erro desconhecido';
  return NextResponse.json({ erro: message }, { status });
}
