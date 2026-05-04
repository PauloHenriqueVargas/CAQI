import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { ContratoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function ContratosPage() {
  const contratos = await fetchService<ContratoDto[]>('financeiro', '/api/v1/financeiro/contratos');

  return (
    <div>
      <header className="mb-6 flex items-baseline justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Contratos (Lei 14.133/2021)</h1>
          <p className="text-sm text-slate-500">
            {contratos.length === 0
              ? 'Nenhum contrato cadastrado.'
              : `${contratos.length} contratos cadastrados.`}
          </p>
        </div>
        <Link
          href="/contratos/novo"
          className="rounded bg-brand px-4 py-2 text-sm font-semibold text-brand-fg hover:bg-brand/90"
        >
          + Novo contrato
        </Link>
      </header>

      {contratos.length > 0 && (
        <div className="overflow-x-auto rounded border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-4 py-2">ID</th>
                <th className="px-4 py-2">Objeto</th>
                <th className="px-4 py-2">Modalidade</th>
                <th className="px-4 py-2">Assinatura</th>
                <th className="px-4 py-2 text-right">Valor global</th>
                <th className="px-4 py-2">PNCP</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {contratos.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50">
                  <td className="px-4 py-2 font-mono text-xs">#{c.id}</td>
                  <td className="px-4 py-2">{c.objeto}</td>
                  <td className="px-4 py-2 text-xs">{c.modalidade}</td>
                  <td className="px-4 py-2 tabular-nums">{fmt.date(c.dataAssinatura)}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(c.valorGlobal)}</td>
                  <td className="px-4 py-2 font-mono text-xs text-slate-400">{c.pncpId ?? '—'}</td>
                  <td className="px-4 py-2">
                    <Link href={`/contratos/${c.id}`} className="text-sm text-brand hover:underline">
                      Detalhe →
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
