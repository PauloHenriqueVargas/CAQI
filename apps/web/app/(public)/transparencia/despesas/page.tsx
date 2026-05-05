import type { Metadata } from 'next';
import Link from 'next/link';

import { Card, StatNumber } from '@/components/Card';
import { fetchPublic } from '@/lib/api-public';
import type { DespesaDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Despesas executadas',
  description:
    'Despesas executadas no exercício, agregadas por natureza PCASP, fonte e siope_grupo. ' +
    'Sem dados pessoais (LGPD art. 11). LRF art. 48-A.',
};

const ANOS_DISPONIVEIS = (() => {
  const cur = new Date().getFullYear();
  return [cur, cur - 1, cur - 2, cur - 3];
})();

export default async function DespesasPublicas({
  searchParams,
}: {
  searchParams: { ano?: string };
}) {
  const ano = searchParams.ano ? parseInt(searchParams.ano, 10) : new Date().getFullYear();
  const despesas = (await fetchPublic<DespesaDto[]>('financeiro', `/api/public/transparencia/despesas?ano=${ano}`)) ?? [];

  const total = despesas.reduce((acc, d) => acc + parseFloat(d.valor || '0'), 0);
  const totalPessoal = despesas.filter((d) => d.isPessoal).reduce((acc, d) => acc + parseFloat(d.valor || '0'), 0);
  const totalCapital = despesas.filter((d) => d.isCapital).reduce((acc, d) => acc + parseFloat(d.valor || '0'), 0);
  const totalMde = despesas.filter((d) => d.siopeGrupo === 'MDE').reduce((acc, d) => acc + parseFloat(d.valor || '0'), 0);

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Execução orçamentária · {ano}
        </p>
        <h1 className="text-2xl font-bold">Despesas executadas</h1>
        <p className="max-w-3xl text-sm text-slate-600">
          Lançamentos do exercício {ano}, agregados por natureza PCASP. Esta página mostra
          dados públicos por natureza — folha individual, CPF/dados pessoais não são publicados.
        </p>
        <nav aria-label="Selecionar ano" className="flex flex-wrap gap-2 pt-2">
          {ANOS_DISPONIVEIS.map((y) => (
            <Link
              key={y}
              href={`/transparencia/despesas?ano=${y}`}
              aria-current={y === ano ? 'page' : undefined}
              className={`rounded border px-3 py-1 text-sm transition ${
                y === ano
                  ? 'border-brand bg-brand text-brand-fg'
                  : 'border-slate-300 bg-white text-slate-700 hover:border-brand hover:text-brand'
              }`}
            >
              {y}
            </Link>
          ))}
        </nav>
      </header>

      <section className="grid gap-4 md:grid-cols-4">
        <Card title="Total executado">
          <StatNumber value={fmt.money(total.toFixed(2))} />
        </Card>
        <Card title="Pessoal" subtitle="natureza 3.1.x">
          <StatNumber value={fmt.money(totalPessoal.toFixed(2))} />
        </Card>
        <Card title="Capital" subtitle="natureza 4.x">
          <StatNumber value={fmt.money(totalCapital.toFixed(2))} />
        </Card>
        <Card title="MDE" subtitle="siope_grupo=MDE">
          <StatNumber value={fmt.money(totalMde.toFixed(2))} />
        </Card>
      </section>

      {despesas.length > 0 ? (
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <caption className="sr-only">Despesas executadas em {ano}</caption>
            <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th scope="col" className="px-4 py-2 text-left">Competência</th>
                <th scope="col" className="px-4 py-2 text-left">Natureza PCASP</th>
                <th scope="col" className="px-4 py-2 text-left">Grupo SIOPE</th>
                <th scope="col" className="px-4 py-2 text-right">Valor</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {despesas.map((d) => (
                <tr key={d.id}>
                  <td className="px-4 py-2 tabular-nums">
                    {d.competencia.slice(4)}/{d.competencia.slice(0, 4)}
                  </td>
                  <td className="px-4 py-2 font-mono text-xs">{d.natureza}</td>
                  <td className="px-4 py-2">{d.siopeGrupo ?? <span className="text-slate-400">—</span>}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(d.valor)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p role="status" className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Nenhuma despesa registrada em {ano}.
        </p>
      )}
    </div>
  );
}
