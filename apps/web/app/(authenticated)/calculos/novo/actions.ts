'use server';

import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';

import { ApiError, fetchService } from '@/lib/api-client';

export type EstadoFormCalculo = {
  erro?: string;
};

export async function criarCalculo(_prev: EstadoFormCalculo, formData: FormData): Promise<EstadoFormCalculo> {
  const ano = Number(formData.get('ano'));
  const escolasRaw = String(formData.get('escolas') ?? '').trim();
  const etapasRaw = String(formData.get('etapas') ?? '').trim();

  if (!ano || !escolasRaw || !etapasRaw) {
    return { erro: 'Preencha ano, escolas e etapas.' };
  }

  const escolas = escolasRaw.split(',').map((s) => s.trim()).filter(Boolean);
  const etapas = etapasRaw.split(',').map((s) => s.trim().toUpperCase()).filter(Boolean);

  try {
    await fetchService<{ ano: number }>('engine', '/api/v1/caqi/calculos', {
      method: 'POST',
      body: JSON.stringify({ ano, escolas, etapas, usarPrecosVigentes: false }),
    });
  } catch (e) {
    if (e instanceof ApiError) {
      return { erro: `Backend respondeu ${e.status}: ${e.body || e.message}` };
    }
    return { erro: e instanceof Error ? e.message : 'Erro desconhecido' };
  }

  revalidatePath('/calculos');
  redirect('/calculos');
}
