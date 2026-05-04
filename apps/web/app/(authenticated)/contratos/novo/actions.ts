'use server';

import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';

import { ApiError, fetchService } from '@/lib/api-client';

export type EstadoFormContrato = { erro?: string };

interface ContratoCriadoDto { id: number }

export async function criarContrato(_prev: EstadoFormContrato, formData: FormData): Promise<EstadoFormContrato> {
  const fornecedorId = Number(formData.get('fornecedorId'));
  const objeto = String(formData.get('objeto') ?? '').trim();
  const dataAssinatura = String(formData.get('dataAssinatura') ?? '').trim();
  const valorGlobal = String(formData.get('valorGlobal') ?? '').trim();
  const modalidade = String(formData.get('modalidade') ?? '').trim();
  const pncpId = String(formData.get('pncpId') ?? '').trim() || null;

  if (!fornecedorId || !objeto || !dataAssinatura || !valorGlobal || !modalidade) {
    return { erro: 'Preencha fornecedor, objeto, data, valor e modalidade.' };
  }

  let criado: ContratoCriadoDto;
  try {
    criado = await fetchService<ContratoCriadoDto>('financeiro', '/api/v1/financeiro/contratos', {
      method: 'POST',
      body: JSON.stringify({ fornecedorId, objeto, dataAssinatura, valorGlobal, modalidade, pncpId }),
    });
  } catch (e) {
    if (e instanceof ApiError) {
      return { erro: `Backend respondeu ${e.status}: ${e.body || e.message}` };
    }
    return { erro: e instanceof Error ? e.message : 'Erro desconhecido' };
  }

  revalidatePath('/contratos');
  redirect(`/contratos/${criado.id}`);
}
