import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';

import { Card, StatNumber } from '@/components/Card';
import { fetchPublic } from '@/lib/api-public';
import type { CalculoCaqDetalheDto, CalculoCaqItemDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Detalhe do cálculo CAQ',
  description: 'Memória item-a-item do cálculo CAQ/CAQi: insumos, custos unitários, divisores e custo por aluno-ano.',
};

export default async function CalculoDetalhePublico({ params }: { params: { id: string } }) {
  const id = Number(params.id);
  if (!Number.isFinite(id)) notFound();

  const calc = await fetchPublic<CalculoCaqDetalheDto>(
    'engine',
    `/api/public/transparencia/calculos/${id}`,
  );
  if (!calc) notFound();

  const itensMin = calc.itens.filter((i) => i.perfil === 'minimo');
  const itensAdq = calc.itens.filter((i) => i.perfil === 'adequado');

  return (
    <div className="space-y-6">
      <p className="text-sm">
        <Link href="/transparencia/calculos" className="text-brand hover:underline">
          ← Cálculos
        </Link>
      </p>

      <header>
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Cálculo #{calc.id} · {calc.ano}
        </p>
        <h1 className="text-2xl font-bold">
          Escola #{calc.escolaId} · Etapa #{calc.etapaId}
        </h1>
      </header>

      <section className="grid gap-4 md:grid-cols-3">
        <Card title="CAQi mínimo" subtitle="por aluno-ano">
          <StatNumber value={fmt.money(calc.valorCaqiAlunoAno)} />
        </Card>
        <Card title="CAQ adequado" subtitle="por aluno-ano">
          <StatNumber value={fmt.money(calc.valorCaqAlunoAno)} />
        </Card>
        <Card title="Gap CAQi → CAQ" variant="warn">
          <StatNumber value={fmt.money(calc.gapAdequado)} />
        </Card>
      </section>

      <ItensTable titulo="Memória — perfil mínimo (CAQi)" itens={itensMin} />
      <ItensTable titulo="Memória — perfil adequado (CAQ)" itens={itensAdq} />
    </div>
  );
}

function ItensTable({ titulo, itens }: { titulo: string; itens: CalculoCaqItemDto[] }) {
  if (itens.length === 0) return null;
  return (
    <section aria-label={titulo} className="space-y-2">
      <h2 className="text-lg font-semibold">{titulo}</h2>
      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <caption className="sr-only">{titulo}</caption>
          <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
            <tr>
              <th scope="col" className="px-3 py-2 text-left">Insumo</th>
              <th scope="col" className="px-3 py-2 text-left">Aplicação</th>
              <th scope="col" className="px-3 py-2 text-right">Qtd</th>
              <th scope="col" className="px-3 py-2 text-right">Custo unit.</th>
              <th scope="col" className="px-3 py-2 text-right">Custo anual</th>
              <th scope="col" className="px-3 py-2 text-right">Divisor</th>
              <th scope="col" className="px-3 py-2 text-right">Custo/aluno-ano</th>
              <th scope="col" className="px-3 py-2 text-left">Base</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {itens.map((i) => (
              <tr key={i.id}>
                <td className="px-3 py-2">
                  <div className="font-medium">{i.insumoNome}</div>
                  <div className="font-mono text-xs text-slate-500">{i.insumoCodigo}</div>
                </td>
                <td className="px-3 py-2 text-xs text-slate-600">{i.tipoAplicacao}</td>
                <td className="px-3 py-2 text-right tabular-nums">{i.qtdAplicada}</td>
                <td className="px-3 py-2 text-right tabular-nums">{fmt.money(i.custoUnitario)}</td>
                <td className="px-3 py-2 text-right tabular-nums">{fmt.money(i.custoAnual)}</td>
                <td className="px-3 py-2 text-right tabular-nums">{i.divisor}</td>
                <td className="px-3 py-2 text-right tabular-nums font-semibold">
                  {fmt.money(i.custoAlunoAno)}
                </td>
                <td className="px-3 py-2 text-xs text-slate-500">{i.baseCalculo}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
