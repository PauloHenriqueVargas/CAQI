import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { FonteRecursoDto } from '@/lib/types';

import { DespesaForm } from './DespesaForm';

export default async function NovaDespesaPage() {
  const fontes = await fetchService<FonteRecursoDto[]>(
    'financeiro',
    '/api/v1/financeiro/fontes-recurso',
  );

  return (
    <div className="max-w-3xl space-y-6">
      <Link href="/despesas" className="text-sm text-brand hover:underline">← Despesas</Link>
      <header>
        <h1 className="text-2xl font-bold">Nova despesa</h1>
        <p className="text-sm text-slate-500">
          Lançamento agregado por competência. Quando o bloqueador está ativo
          (<code>caqi.compliance.bloquear-empenhos-violadores=true</code>),
          despesas que derrubam alguma vinculação legal recebem 409 Conflict —
          rollback automático.
        </p>
      </header>
      <DespesaForm fontes={fontes} />
    </div>
  );
}
