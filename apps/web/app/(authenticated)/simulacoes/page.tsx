import Link from 'next/link';

export default function SimulacoesPage() {
  return (
    <div className="max-w-3xl space-y-6">
      <header>
        <h1 className="text-2xl font-bold">Simulador "e se?"</h1>
        <p className="text-sm text-slate-500">
          Recalcula CAQ/CAQi com cenários hipotéticos sem persistir nada. Útil para
          avaliar impacto de mudanças em políticas educacionais.
        </p>
      </header>

      <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold">Cenários típicos</h2>
        <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-slate-700">
          <li>Reduzir alunos por turma do EF1 de 25 → 20 (impacta divisor de PES-001)</li>
          <li>Aumentar piso salarial do professor (multiplicador de custo em PES-001)</li>
          <li>Dobrar quantidade de docentes por turma (qtd_padrao de PES-001)</li>
          <li>Combinar múltiplas mudanças e ver efeito agregado</li>
        </ul>

        <Link
          href="/simulacoes/nova"
          className="mt-5 inline-block rounded bg-brand px-5 py-2 font-semibold text-brand-fg hover:bg-brand/90"
        >
          Nova simulação →
        </Link>
      </section>
    </div>
  );
}
