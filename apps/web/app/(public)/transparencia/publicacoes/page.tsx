import type { Metadata } from 'next';

import { fetchPublic } from '@/lib/api-public';
import type { PublicacaoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Publicações (LRF art. 48-A)',
  description:
    'Trilha de publicações automáticas de transparência ativa: snapshots periódicos do conteúdo público ' +
    'com hash SHA-256 para verificação de integridade. LRF art. 48-A.',
};

const TIPO_LABEL: Record<string, string> = {
  fundeb_execucao: 'Fundeb / MDE / VAAT',
  siope_quadro:    'Quadro SIOPE',
  calculos_caq:    'Cálculos CAQ',
  contratos:       'Contratos',
  despesas:        'Despesas',
};

export default async function PublicacoesPublicas({
  searchParams,
}: {
  searchParams: { tipo?: string };
}) {
  const tipoFiltro = searchParams.tipo ?? '';
  const qs = tipoFiltro ? `?tipo=${encodeURIComponent(tipoFiltro)}` : '';
  const lista = (await fetchPublic<PublicacaoDto[]>('compliance', `/api/public/transparencia/publicacoes${qs}`)) ?? [];

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          LRF art. 48-A · Transparência ativa
        </p>
        <h1 className="text-2xl font-bold">Trilha de publicações</h1>
        <p className="max-w-3xl text-sm text-slate-600">
          Cada linha abaixo representa um snapshot publicado automaticamente pelo sistema. O
          <strong> hash SHA-256</strong> permite verificar que o conteúdo arquivado não foi alterado
          após a publicação. Conteúdo idêntico ao último publicado não gera nova entrada
          (idempotência).
        </p>
      </header>

      <nav aria-label="Filtrar por tipo" className="flex flex-wrap gap-2">
        <FilterLink tipo="" current={tipoFiltro} label="Todos" />
        {Object.entries(TIPO_LABEL).map(([k, v]) => (
          <FilterLink key={k} tipo={k} current={tipoFiltro} label={v} />
        ))}
      </nav>

      {lista.length === 0 ? (
        <p role="status" className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-500">
          {tipoFiltro
            ? `Nenhuma publicação registrada para o tipo "${tipoFiltro}".`
            : 'Ainda não há publicações registradas. O scheduler executa a cada 6 horas.'}
        </p>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="w-full text-sm">
            <caption className="sr-only">Trilha de publicações automáticas LRF art. 48-A</caption>
            <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th scope="col" className="px-4 py-2 text-left">ID</th>
                <th scope="col" className="px-4 py-2 text-left">Tipo</th>
                <th scope="col" className="px-4 py-2 text-left">Referência</th>
                <th scope="col" className="px-4 py-2 text-left">Publicado em</th>
                <th scope="col" className="px-4 py-2 text-right">Tamanho</th>
                <th scope="col" className="px-4 py-2 text-left">Hash SHA-256</th>
                <th scope="col" className="px-4 py-2 text-left">URL pública</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {lista.map((p) => (
                <tr key={p.id}>
                  <td className="px-4 py-2 tabular-nums text-slate-500">#{p.id}</td>
                  <td className="px-4 py-2">
                    <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700">
                      {TIPO_LABEL[p.tipo] ?? p.tipo}
                    </span>
                  </td>
                  <td className="px-4 py-2 tabular-nums">{p.referencia || '—'}</td>
                  <td className="px-4 py-2 tabular-nums">
                    {fmt.date(p.dataPublicacao)}{' '}
                    <span className="text-xs text-slate-400">
                      {new Date(p.dataPublicacao).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-right tabular-nums">
                    {(p.tamanhoBytes ?? 0).toLocaleString('pt-BR')} B
                  </td>
                  <td className="px-4 py-2 font-mono text-xs">
                    <abbr title={p.conteudoHash} className="cursor-help no-underline">
                      {p.conteudoHash?.slice(0, 12) ?? '—'}…
                    </abbr>
                  </td>
                  <td className="px-4 py-2">
                    {p.urlPublica ? (
                      <a href={p.urlPublica} className="text-brand hover:underline" rel="bookmark">
                        ver portal →
                      </a>
                    ) : (
                      <span className="text-slate-400">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <section className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-600">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">
          Como verificar a integridade
        </h2>
        <ol className="mt-2 list-decimal space-y-1 pl-5">
          <li>Solicite ao SIC do município o <strong>snapshot JSON</strong> da publicação alvo.</li>
          <li>
            Calcule o SHA-256 do JSON canônico (chaves em ordem alfabética, sem indentação) com
            qualquer ferramenta criptográfica (<code>openssl dgst -sha256</code> ou equivalente).
          </li>
          <li>Compare com o hash listado nesta página.</li>
          <li>Se diferente, o snapshot foi alterado após a publicação — denuncie ao TCE/MP.</li>
        </ol>
      </section>
    </div>
  );
}

function FilterLink({
  tipo,
  current,
  label,
}: {
  tipo: string;
  current: string;
  label: string;
}) {
  const isCurrent = tipo === current;
  const href = tipo ? `/transparencia/publicacoes?tipo=${encodeURIComponent(tipo)}` : '/transparencia/publicacoes';
  return (
    <a
      href={href}
      aria-current={isCurrent ? 'page' : undefined}
      className={`rounded border px-3 py-1 text-sm transition ${
        isCurrent
          ? 'border-brand bg-brand text-brand-fg'
          : 'border-slate-300 bg-white text-slate-700 hover:border-brand hover:text-brand'
      }`}
    >
      {label}
    </a>
  );
}
