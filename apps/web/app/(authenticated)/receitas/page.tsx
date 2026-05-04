import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { ReceitaDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function ReceitasPage({
  searchParams,
}: {
  searchParams: { ano?: string };
}) {
  const ano = searchParams.ano ? parseInt(searchParams.ano, 10) : new Date().getFullYear();
  const receitas = await fetchService<ReceitaDto[]>(
    'financeiro',
    `/api/v1/financeiro/receitas?ano=${ano}`,
  );

  const totalPorOrigem = receitas.reduce<Record<string, number>>((acc, r) => {
    const o = r.origem || 'indefinida';
    acc[o] = (acc[o] ?? 0) + parseFloat(r.valor);
    return acc;
  }, {});

  return (
    <div>
      <header className="mb-6 flex items-baseline justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Receitas · {ano}</h1>
          <p className="text-sm text-slate-500">
            {receitas.length === 0
              ? 'Nenhuma receita cadastrada para este ano.'
              : `${receitas.length} lançamentos.`}
          </p>
        </div>
        <Link
          href="/receitas/nova"
          className="rounded bg-brand px-4 py-2 text-sm font-semibold text-brand-fg hover:bg-brand/90"
        >
          + Nova receita
        </Link>
      </header>

      {Object.keys(totalPorOrigem).length > 0 && (
        <section className="mb-6 grid gap-3 sm:grid-cols-3 md:grid-cols-6">
          {Object.entries(totalPorOrigem).map(([origem, total]) => (
            <div key={origem} className="rounded border border-slate-200 bg-white p-3">
              <p className="text-xs uppercase text-slate-500">{origem}</p>
              <p className="mt-1 text-sm font-semibold tabular-nums">{fmt.money(total)}</p>
            </div>
          ))}
        </section>
      )}

      {receitas.length > 0 && (
        <div className="overflow-x-auto rounded border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-4 py-2">ID</th>
                <th className="px-4 py-2">Competência</th>
                <th className="px-4 py-2">Origem</th>
                <th className="px-4 py-2">PCASP</th>
                <th className="px-4 py-2 text-right">Valor</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {receitas.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-2 font-mono text-xs">#{r.id}</td>
                  <td className="px-4 py-2 tabular-nums">{r.competencia}</td>
                  <td className="px-4 py-2">{r.origem}</td>
                  <td className="px-4 py-2 font-mono text-xs">{r.pcasp ?? '—'}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(r.valor)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
