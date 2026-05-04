/**
 * Gauge minimalista para vinculações constitucionais/legais.
 * Mostra: valor executado vs mínimo, com cor verde/vermelho conforme cumpre.
 */
export function Gauge({
  label,
  executado,
  minimo,
  baseLegal,
  cumpre,
}: {
  label: string;
  executado: string; // "82.14"
  minimo: string;    // "70"
  baseLegal: string;
  cumpre: boolean;
}) {
  const exec = parseFloat(executado);
  const min = parseFloat(minimo);
  // Largura da barra: cap em 150% do mínimo para não esticar demais
  const cap = min * 1.5;
  const pct = Math.min(100, (exec / cap) * 100);
  const minPct = (min / cap) * 100;

  return (
    <article
      className={`rounded-lg border-2 bg-white p-5 ${
        cumpre ? 'border-success/40' : 'border-danger/60'
      }`}
    >
      <header className="flex items-baseline justify-between">
        <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-600">{label}</h3>
        <span
          className={`rounded px-2 py-0.5 text-xs font-semibold ${
            cumpre ? 'bg-success/15 text-success' : 'bg-danger/15 text-danger'
          }`}
        >
          {cumpre ? 'CUMPRE' : 'NÃO CUMPRE'}
        </span>
      </header>

      <div className="mt-4 flex items-baseline gap-2">
        <span className="text-3xl font-bold tabular-nums">{executado}</span>
        <span className="text-sm text-slate-500">% executado</span>
        <span className="ml-auto text-xs text-slate-400">mín. {minimo}%</span>
      </div>

      <div className="relative mt-3 h-3 w-full rounded-full bg-slate-100">
        {/* Linha do mínimo legal */}
        <div
          className="absolute top-0 h-3 w-0.5 bg-slate-600"
          style={{ left: `${minPct}%` }}
          aria-label={`Mínimo legal: ${minimo}%`}
        />
        {/* Barra do executado */}
        <div
          className={`h-3 rounded-full ${cumpre ? 'bg-success' : 'bg-danger'}`}
          style={{ width: `${pct}%` }}
        />
      </div>

      <p className="mt-2 text-xs text-slate-400">{baseLegal}</p>
    </article>
  );
}
