'use server';

import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';

import { ApiError, fetchService } from '@/lib/api-client';

export type EstadoFormDespesa = {
  erro?: string;
  bloqueado?: string;
};

export async function criarDespesa(
  _prev: EstadoFormDespesa,
  formData: FormData,
): Promise<EstadoFormDespesa> {
  const competencia = String(formData.get('competencia') ?? '').trim();
  const valor = String(formData.get('valor') ?? '').trim();
  const natureza = String(formData.get('natureza') ?? '').trim();
  const fonteRecursoIdStr = String(formData.get('fonteRecursoId') ?? '').trim();
  const fonteRecursoId = fonteRecursoIdStr ? Number(fonteRecursoIdStr) : null;
  const pcasp = String(formData.get('pcasp') ?? '').trim() || null;
  const siopeGrupo = String(formData.get('siopeGrupo') ?? '').trim() || null;

  if (!competencia || !valor || !natureza) {
    return { erro: 'Preencha competência, valor e natureza PCASP.' };
  }
  if (!/^\d{6}$/.test(competencia)) {
    return { erro: 'Competência deve ser YYYYMM (6 dígitos).' };
  }

  try {
    await fetchService('financeiro', '/api/v1/financeiro/despesas', {
      method: 'POST',
      body: JSON.stringify({ competencia, valor, natureza, fonteRecursoId, pcasp, siopeGrupo }),
    });
  } catch (e) {
    if (e instanceof ApiError) {
      // 409 Conflict → EmpenhoBloqueadoException (bloqueador ativo)
      if (e.status === 409) {
        const motivo = extrairMotivo(e.body) ?? 'Despesa derruba alguma vinculação legal.';
        return { bloqueado: motivo };
      }
      return { erro: `Backend respondeu ${e.status}: ${e.body || e.message}` };
    }
    return { erro: e instanceof Error ? e.message : 'Erro desconhecido' };
  }

  const ano = competencia.substring(0, 4);
  revalidatePath('/despesas');
  redirect(`/despesas?ano=${ano}`);
}

function extrairMotivo(body: string | undefined): string | null {
  if (!body) return null;
  // Spring devolve algo como {"timestamp":"...","status":409,"error":"Conflict","message":"Empenho bloqueado: ...","path":"..."}
  try {
    const json = JSON.parse(body);
    return typeof json.message === 'string' ? json.message : null;
  } catch {
    return body.slice(0, 280);
  }
}
