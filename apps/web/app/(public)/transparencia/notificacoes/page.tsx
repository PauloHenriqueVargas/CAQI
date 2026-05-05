import type { Metadata } from 'next';
import Link from 'next/link';

import { fetchPublic } from '@/lib/api-public';
import type { NotificacaoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Notificações de compliance',
  description:
    'Histórico público de notificações automáticas quando vinculações legais não foram cumpridas — ' +
    'controle social pode auditar (CACS-Fundeb, CME). LAI.',
};

const ANOS_DISPONIVEIS = (() => {
  const cur = new Date().getFullYear();
  return [cur, cur - 1, cur - 2];
})();

const SEVERIDADE_BG: Record<NotificacaoDto['severidade'], string> = {
  info:    'bg-slate-100 text-slate-700 border-slate-300',
  warn:    'bg-warning/15 text-warning border-warning/40',
  alta:    'bg-warning/25 text-warning border-warning/60',
  critica: 'bg-danger/10 text-danger border-danger/40',
};

const STATUS_BG: Record<NotificacaoDto['status'], string> = {
  aberta:     'bg-danger/10 text-danger',
  em_analise: 'bg-warning/15 text-warning',
  resolvida:  'bg-success/15 text-success',
  ignorada:   'bg-slate-100 text-slate-600',
};

export default async function NotificacoesPublicas({
  searchParams,
}: {
  searchParams: { ano?: string };
}) {
  const ano = searchParams.ano ? parseInt(searchParams.ano, 10) : new Date().getFullYear();
  const lista = (await fetchPublic<NotificacaoDto[]>('compliance', `/api/public/transparencia/notificacoes?ano=${ano}`)) ?? [];

  const abertas = lista.filter((n) => n.status === 'aberta').length;
  const resolvidas = lista.filter((n) => n.status === 'resolvida').length;

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Compliance · Controle social
        </p>
        <h1 className="text-2xl font-bold">Notificações de compliance · {ano}</h1>
        <p className="max-w-3xl text-sm text-slate-600">
          O sistema gera automaticamente uma notificação sempre que uma vinculação legal
          (MDE 25%, Fundeb 70% pessoal, VAAT 15% capital) deixa de ser cumprida. Cada notificação
          tem severidade, base legal e status. CACS-Fundeb e CME podem auditar este histórico
          publicamente.
        </p>

        <nav aria-label="Selecionar ano" className="flex flex-wrap gap-2 pt-2">
          {ANOS_DISPONIVEIS.map((y) => (
            <Link
              key={y}
              href={`/transparencia/notificacoes?ano=${y}`}
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

      <div className="flex flex-wrap gap-4 text-sm">
        <span className="rounded border border-slate-200 bg-white px-3 py-1.5">
          Total: <strong>{lista.length}</strong>
        </span>
        <span className="rounded border border-danger/40 bg-danger/5 px-3 py-1.5 text-danger">
          Abertas: <strong>{abertas}</strong>
        </span>
        <span className="rounded border border-success/40 bg-success/5 px-3 py-1.5 text-success">
          Resolvidas: <strong>{resolvidas}</strong>
        </span>
      </div>

      {lista.length === 0 ? (
        <p role="status" className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-500">
          Nenhuma notificação registrada em {ano}.
        </p>
      ) : (
        <ul className="space-y-3">
          {lista.map((n) => (
            <li
              key={n.id}
              className={`rounded-lg border-2 bg-white p-4 ${SEVERIDADE_BG[n.severidade] ?? SEVERIDADE_BG.info}`}
            >
              <header className="flex flex-wrap items-baseline gap-2">
                <h2 className="font-semibold">{n.titulo}</h2>
                <span className={`rounded px-2 py-0.5 text-xs font-semibold ${STATUS_BG[n.status]}`}>
                  {n.status.replace('_', ' ')}
                </span>
                <span className="rounded bg-white/60 px-2 py-0.5 text-xs uppercase tracking-wider">
                  {n.severidade}
                </span>
                <span className="ml-auto text-xs text-slate-500">{fmt.date(n.createdAt)}</span>
              </header>
              <p className="mt-2 text-sm text-slate-700">{n.descricao}</p>
              {n.baseLegal && (
                <p className="mt-2 text-xs text-slate-500">
                  <strong>Base legal:</strong> {n.baseLegal}
                </p>
              )}
              {n.resolvedAt && (
                <p className="mt-1 text-xs text-success">Resolvida em {fmt.date(n.resolvedAt)}</p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
