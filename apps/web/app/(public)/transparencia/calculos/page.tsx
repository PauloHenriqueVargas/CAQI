import type { Metadata } from 'next';
import Link from 'next/link';

import { fetchPublic } from '@/lib/api-public';
import type { CalculoCaqResumoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Cálculos CAQ/CAQi por escola',
  description:
    'Custo Aluno Qualidade (CAQi mínimo e CAQ adequado) calculado escola por escola, ' +
    'com gap para o padrão de qualidade. PNE meta 20.7.',
};

export default async function CalculosPublicos() {
  const calculos = (await fetchPublic<CalculoCaqResumoDto[]>('engine', '/api/public/transparencia/calculos')) ?? [];

  return (
    <div className="space-y-6">
      <header>
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          PNE · Meta 20.7
        </p>
        <h1 className="text-2xl font-bold">Cálculos CAQ/CAQi</h1>
        <p className="mt-1 max-w-3xl text-sm text-slate-600">
          O <strong>Custo Aluno Qualidade Inicial (CAQi)</strong> é o valor mínimo por aluno para
          assegurar padrão básico de qualidade da educação. O <strong>CAQ adequado</strong> reflete
          o padrão pleno. O gap entre os dois indica o investimento necessário para atingir o
          padrão de qualidade da educação básica.
        </p>
      </header>

      {calculos.length === 0 ? (
        <p role="status" className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Ainda não há cálculos publicados.
        </p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <caption className="sr-only">Cálculos CAQ/CAQi persistidos por escola/etapa</caption>
            <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th scope="col" className="px-4 py-2 text-left">ID</th>
                <th scope="col" className="px-4 py-2 text-left">Ano</th>
                <th scope="col" className="px-4 py-2 text-left">Escola</th>
                <th scope="col" className="px-4 py-2 text-left">Etapa</th>
                <th scope="col" className="px-4 py-2 text-right">CAQi mínimo</th>
                <th scope="col" className="px-4 py-2 text-right">CAQ adequado</th>
                <th scope="col" className="px-4 py-2 text-right">Gap</th>
                <th scope="col" className="px-4 py-2 text-left"><span className="sr-only">Ações</span></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {calculos.map((c) => (
                <tr key={c.id}>
                  <td className="px-4 py-2 tabular-nums text-slate-500">#{c.id}</td>
                  <td className="px-4 py-2 tabular-nums">{c.ano}</td>
                  <td className="px-4 py-2 tabular-nums">Escola #{c.escolaId}</td>
                  <td className="px-4 py-2 tabular-nums">Etapa #{c.etapaId}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(c.valorCaqiAlunoAno)}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(c.valorCaqAlunoAno)}</td>
                  <td className="px-4 py-2 text-right tabular-nums text-warning">
                    {fmt.money(c.gapAdequado)}
                  </td>
                  <td className="px-4 py-2">
                    <Link
                      href={`/transparencia/calculos/${c.id}`}
                      className="text-brand hover:underline"
                      aria-label={`Ver memória do cálculo ${c.id}`}
                    >
                      Memória →
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
