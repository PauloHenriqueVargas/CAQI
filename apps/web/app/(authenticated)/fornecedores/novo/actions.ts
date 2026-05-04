'use server';

import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';

import { ApiError, fetchService } from '@/lib/api-client';

export type EstadoFormFornecedor = { erro?: string };

export async function criarFornecedor(
  _prev: EstadoFormFornecedor,
  formData: FormData,
): Promise<EstadoFormFornecedor> {
  const cnpj = String(formData.get('cnpj') ?? '').trim();
  const nome = String(formData.get('nome') ?? '').trim();
  const optanteSimples = formData.get('optanteSimples') === 'on';
  const municipio = String(formData.get('municipio') ?? '').trim() || null;

  if (!cnpj || !nome) {
    return { erro: 'Preencha CNPJ e nome.' };
  }

  try {
    await fetchService('financeiro', '/api/v1/financeiro/fornecedores', {
      method: 'POST',
      body: JSON.stringify({ cnpj, nome, optanteSimples, municipio }),
    });
  } catch (e) {
    if (e instanceof ApiError) {
      return { erro: `Backend respondeu ${e.status}: ${e.body || e.message}` };
    }
    return { erro: e instanceof Error ? e.message : 'Erro desconhecido' };
  }

  revalidatePath('/fornecedores');
  redirect('/fornecedores');
}
