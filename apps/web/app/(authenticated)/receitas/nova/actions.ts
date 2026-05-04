'use server';

import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';

import { ApiError, fetchService } from '@/lib/api-client';

export type EstadoFormReceita = { erro?: string };

export async function criarReceita(
  _prev: EstadoFormReceita,
  formData: FormData,
): Promise<EstadoFormReceita> {
  const competencia = String(formData.get('competencia') ?? '').trim();
  const valor = String(formData.get('valor') ?? '').trim();
  const origem = String(formData.get('origem') ?? '').trim();
  const pcasp = String(formData.get('pcasp') ?? '').trim() || null;

  if (!competencia || !valor || !origem) {
    return { erro: 'Preencha competência, valor e origem.' };
  }
  if (!/^\d{6}$/.test(competencia)) {
    return { erro: 'Competência deve ser YYYYMM (6 dígitos).' };
  }

  try {
    await fetchService('financeiro', '/api/v1/financeiro/receitas', {
      method: 'POST',
      body: JSON.stringify({ competencia, valor, origem, pcasp }),
    });
  } catch (e) {
    if (e instanceof ApiError) {
      return { erro: `Backend respondeu ${e.status}: ${e.body || e.message}` };
    }
    return { erro: e instanceof Error ? e.message : 'Erro desconhecido' };
  }

  const ano = competencia.substring(0, 4);
  revalidatePath('/receitas');
  redirect(`/receitas?ano=${ano}`);
}
