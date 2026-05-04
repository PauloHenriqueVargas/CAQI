import { fetchService } from '@/lib/api-client';
import type { NotificacaoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

const SEV_STYLES: Record<NotificacaoDto['severidade'], string> = {
  critica: 'border-danger bg-danger/5',
  alta:    'border-warning bg-warning/5',
  warn:    'border-slate-400 bg-slate-50',
  info:    'border-slate-300 bg-white',
};

export default async function NotificacoesPage({
  searchParams,
}: {
  searchParams: { status?: string };
}) {
  const status = searchParams.status ?? 'aberta';
  const notificacoes = await fetchService<NotificacaoDto[]>(
    'compliance',
    `/api/v1/compliance/notificacoes?status=${encodeURIComponent(status)}`,
  );

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold">Notificações de compliance</h1>
        <p className="text-sm text-slate-500">
          Violações automáticas detectadas pelo AvaliadorComplianceService — fechar com{' '}
          <code className="rounded bg-slate-100 px-1">PATCH /notificacoes/&#123;id&#125;/status</code>.
        </p>
      </header>

      <nav aria-label="Filtros" className="flex gap-2 text-sm">
        {['aberta', 'em_analise', 'resolvida', 'ignorada'].map((s) => (
          <a
            key={s}
            href={`?status=${s}`}
            className={`rounded px-3 py-1 ${
              s === status ? 'bg-brand text-brand-fg' : 'bg-white border border-slate-200 hover:bg-slate-100'
            }`}
          >
            {s.replace('_', ' ')}
          </a>
        ))}
      </nav>

      {notificacoes.length === 0 ? (
        <p className="rounded border border-success/30 bg-success/5 p-4 text-sm">
          Nenhuma notificação <strong>{status.replace('_', ' ')}</strong> no momento.
        </p>
      ) : (
        <ul className="space-y-3">
          {notificacoes.map((n) => (
            <li
              key={n.id}
              className={`rounded border-l-4 p-4 shadow-sm ${SEV_STYLES[n.severidade]}`}
            >
              <header className="flex items-baseline justify-between gap-4">
                <div>
                  <h3 className="font-semibold">{n.titulo}</h3>
                  <p className="text-xs text-slate-500">
                    {n.tipo} · severidade <strong>{n.severidade}</strong> · ano {n.anoReferencia}
                  </p>
                </div>
                <time className="text-xs text-slate-400" dateTime={n.createdAt}>
                  {fmt.date(n.createdAt)}
                </time>
              </header>
              <p className="mt-2 text-sm text-slate-700">{n.descricao}</p>
              {n.baseLegal && (
                <p className="mt-1 text-xs text-slate-500">Base legal: {n.baseLegal}</p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
