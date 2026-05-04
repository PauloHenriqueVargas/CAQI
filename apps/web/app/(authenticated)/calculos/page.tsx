import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { CalculoCaqResumoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function CalculosPage() {
  const calculos = await fetchService<CalculoCaqResumoDto[]>('engine', '/api/v1/caqi/calculos');

  return (
    <div>
      <header className="mb-6 flex items-baseline justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Cálculos CAQ/CAQi</h1>
          <p className="text-sm text-slate-500">
            {calculos.length === 0
              ? 'Nenhum cálculo persistido ainda.'
              : `${calculos.length} cálculos persistidos.`}
          </p>
        </div>
        <Link
          href="/calculos/novo"
          className="rounded bg-brand px-4 py-2 text-sm font-semibold text-brand-fg hover:bg-brand/90"
        >
          + Novo cálculo
        </Link>
      </header>

      {calculos.length > 0 && (
        <div className="overflow-x-auto rounded border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-4 py-2">ID</th>
                <th className="px-4 py-2">Escola</th>
                <th className="px-4 py-2">Etapa</th>
                <th className="px-4 py-2">Ano</th>
                <th className="px-4 py-2 text-right">CAQi</th>
                <th className="px-4 py-2 text-right">CAQ adequado</th>
                <th className="px-4 py-2 text-right">Gap</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {calculos.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50">
                  <td className="px-4 py-2 font-mono text-xs">#{c.id}</td>
                  <td className="px-4 py-2">{c.escolaId}</td>
                  <td className="px-4 py-2">{c.etapaId}</td>
                  <td className="px-4 py-2 tabular-nums">{c.ano}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(c.valorCaqiAlunoAno)}</td>
                  <td className="px-4 py-2 text-right tabular-nums">{fmt.money(c.valorCaqAlunoAno)}</td>
                  <td className="px-4 py-2 text-right tabular-nums text-warning">{fmt.money(c.gapAdequado)}</td>
                  <td className="px-4 py-2">
                    <Link
                      href={`/calculos/${c.id}`}
                      className="text-sm text-brand hover:underline"
                    >
                      Ver memória →
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
