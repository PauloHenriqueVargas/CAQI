import Link from 'next/link';

import { fetchService } from '@/lib/api-client';
import type { CalculoCaqDetalheDto, CalculoCaqItemDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function CalculoDetalhePage({ params }: { params: { id: string } }) {
  const calc = await fetchService<CalculoCaqDetalheDto>(
    'engine',
    `/api/v1/caqi/calculos/${params.id}`,
  );

  const minimo = calc.itens.filter((i) => i.perfil === 'minimo');
  const adequado = calc.itens.filter((i) => i.perfil === 'adequado');

  return (
    <div className="space-y-6">
      <Link href="/calculos" className="text-sm text-brand hover:underline">← Cálculos</Link>

      <header>
        <h1 className="text-2xl font-bold">Cálculo #{calc.id}</h1>
        <p className="text-sm text-slate-500">
          Escola {calc.escolaId} · Etapa {calc.etapaId} · Ano {calc.ano}
        </p>
      </header>

      <section className="grid gap-4 md:grid-cols-3">
        <Stat label="CAQi (mínimo)" value={fmt.money(calc.valorCaqiAlunoAno)} />
        <Stat label="CAQ (adequado)" value={fmt.money(calc.valorCaqAlunoAno)} />
        <Stat label="Gap" value={fmt.money(calc.gapAdequado)} variant="warn" />
      </section>

      <section>
        <h2 className="mb-2 text-lg font-semibold">Memória de cálculo — perfil mínimo (CAQi)</h2>
        <MemoriaTable itens={minimo} />
      </section>

      <section>
        <h2 className="mb-2 text-lg font-semibold">Memória de cálculo — perfil adequado (CAQ)</h2>
        <MemoriaTable itens={adequado} />
      </section>
    </div>
  );
}

function Stat({ label, value, variant }: { label: string; value: string; variant?: 'warn' }) {
  const accent = variant === 'warn' ? 'border-warning' : 'border-slate-200';
  return (
    <div className={`rounded-lg border-2 ${accent} bg-white p-5`}>
      <p className="text-sm font-semibold uppercase tracking-wider text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-bold tabular-nums">{value}</p>
    </div>
  );
}

function MemoriaTable({ itens }: { itens: CalculoCaqItemDto[] }) {
  return (
    <div className="overflow-x-auto rounded border border-slate-200 bg-white">
      <table className="w-full text-sm">
        <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase tracking-wider text-slate-500">
          <tr>
            <th className="px-3 py-2">Insumo</th>
            <th className="px-3 py-2">Tipo</th>
            <th className="px-3 py-2 text-right">Qtd</th>
            <th className="px-3 py-2 text-right">Custo unit.</th>
            <th className="px-3 py-2 text-right">Custo anual</th>
            <th className="px-3 py-2 text-right">Divisor</th>
            <th className="px-3 py-2 text-right">R$/aluno/ano</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {itens.map((it) => (
            <tr key={it.id}>
              <td className="px-3 py-2">
                <span className="font-mono text-xs text-slate-500">{it.insumoCodigo}</span>
                <span className="ml-2">{it.insumoNome}</span>
              </td>
              <td className="px-3 py-2 text-xs">{it.tipoAplicacao}</td>
              <td className="px-3 py-2 text-right tabular-nums">{it.qtdAplicada}</td>
              <td className="px-3 py-2 text-right tabular-nums">{fmt.money(it.custoUnitario)}</td>
              <td className="px-3 py-2 text-right tabular-nums">{fmt.money(it.custoAnual)}</td>
              <td className="px-3 py-2 text-right tabular-nums">{it.divisor}</td>
              <td className="px-3 py-2 text-right tabular-nums font-semibold">
                {fmt.money(it.custoAlunoAno)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
