import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { DespesaDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function DespesasPage({
  searchParams,
}: {
  searchParams: { ano?: string };
}) {
  const ano = searchParams.ano ? parseInt(searchParams.ano, 10) : new Date().getFullYear();
  const despesas = await fetchService<DespesaDto[]>(
    'financeiro',
    `/api/v1/financeiro/despesas?ano=${ano}`,
  );

  const totals = despesas.reduce(
    (acc, d) => {
      const v = parseFloat(d.valor);
      acc.total += v;
      if (d.isPessoal) acc.pessoal += v;
      if (d.isCapital) acc.capital += v;
      if (d.siopeGrupo === 'MDE') acc.mde += v;
      return acc;
    },
    { total: 0, pessoal: 0, capital: 0, mde: 0 },
  );

  return (
    <div>
      <header className="mb-6 flex items-baseline justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Despesas · {ano}</h1>
          <p className="text-sm text-slate-500">
            {despesas.length === 0
              ? 'Nenhuma despesa lançada para este ano.'
              : `${despesas.length} lançamentos.`}
          </p>
        </div>
        <Link
          href="/despesas/nova"
          className="rounded bg-brand px-4 py-2 text-sm font-semibold text-brand-fg hover:bg-brand/90"
        >
          + Nova despesa
        </Link>
      </header>

      {despesas.length > 0 && (
        <section className="mb-6 grid gap-3 sm:grid-cols-2 md:grid-cols-4">
          <Card label="Total" value={fmt.money(totals.total)} />
          <Card label="Pessoal (3.1.x)" value={fmt.money(totals.pessoal)} />
          <Card label="Capital (4.x)" value={fmt.money(totals.capital)} />
          <Card label="MDE" value={fmt.money(totals.mde)} />
        </section>
      )}

      {despesas.length > 0 && (
        <div className="overflow-x-auto rounded border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-4 py-2">ID</th>
                <th className="px-4 py-2">Competência</th>
                <th className="px-4 py-2">Natureza</th>
                <th className="px-4 py-2">SIOPE</th>
                <th className="px-4 py-2">Fonte</th>
                <th className="px-4 py-2">Classe</th>
                <th className="px-4 py-2 text-right">Valor</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {despesas.map((d) => (
                <tr key={d.id}>
                  <td className="px-4 py-2 font-mono text-xs">#{d.id}</td>
                  <td className="px-4 py-2 tabular-nums">{d.competencia}</td>
                  <td className="px-4 py-2 font-mono text-xs">{d.natureza}</td>
                  <td className="px-4 py-2 text-xs">{d.siopeGrupo ?? '—'}</td>
                  <td className="px-4 py-2 text-xs">{d.fonteRecursoId ?? '—'}</td>
                  <td className="px-4 py-2 text-xs">
                    {d.isPessoal ? 'pessoal' : d.isCapital ? 'capital' : 'outras'}
                  </td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(d.valor)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded border border-slate-200 bg-white p-3">
      <p className="text-xs uppercase text-slate-500">{label}</p>
      <p className="mt-1 font-semibold tabular-nums">{value}</p>
    </div>
  );
}
