import type { Metadata } from 'next';
import Link from 'next/link';

import { Card, StatNumber } from '@/components/Card';
import { Gauge } from '@/components/Gauge';
import { fetchPublic } from '@/lib/api-public';
import type {
  CalculoCaqResumoDto,
  ContratoDto,
  ExecucaoFundebDto,
  NotificacaoDto,
  PublicacaoDto,
} from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Painel CACS-Fundeb / CME',
  description:
    'Visão consolidada para Conselho de Acompanhamento e Controle Social do Fundeb (CACS-Fundeb) ' +
    'e Conselho Municipal de Educação (CME) — vinculações legais, alertas, publicações e cálculos CAQ.',
};

export default async function PainelConselhos() {
  const ano = new Date().getFullYear();

  const [fundeb, calculos, contratos, notificacoes, publicacoes] = await Promise.all([
    fetchPublic<ExecucaoFundebDto>('financeiro', `/api/public/transparencia/fundeb-execucao?ano=${ano}`),
    fetchPublic<CalculoCaqResumoDto[]>('engine', '/api/public/transparencia/calculos'),
    fetchPublic<ContratoDto[]>('financeiro', '/api/public/transparencia/contratos'),
    fetchPublic<NotificacaoDto[]>('compliance', `/api/public/transparencia/notificacoes?ano=${ano}`),
    fetchPublic<PublicacaoDto[]>('compliance', '/api/public/transparencia/publicacoes'),
  ]);

  const abertas = (notificacoes ?? []).filter((n) => n.status === 'aberta');
  const criticas = abertas.filter((n) => n.severidade === 'critica');
  const totalContratado = (contratos ?? []).reduce(
    (acc, c) => acc + parseFloat(c.valorGlobal || '0'),
    0,
  );
  const ultimasPublicacoes = (publicacoes ?? []).slice(0, 5);
  const recentNotificacoes = (notificacoes ?? []).slice(0, 5);

  return (
    <div className="space-y-10">
      <header className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Controle social · Exercício {ano}
        </p>
        <h1 className="text-3xl font-bold tracking-tight">Painel CACS-Fundeb / CME</h1>
        <p className="max-w-3xl text-sm text-slate-600">
          Visão consolidada para o <strong>Conselho de Acompanhamento e Controle Social do Fundeb</strong>{' '}
          (Lei 14.113/2020 art. 33) e o <strong>Conselho Municipal de Educação</strong> (LDB art. 11).
          Reúne, em uma página, vinculações legais, alertas em aberto, cálculos CAQ por escola e
          a trilha de publicações automáticas (LRF art. 48-A).
        </p>
      </header>

      {fundeb && (
        <section aria-labelledby="vinculacoes" className="space-y-3">
          <h2 id="vinculacoes" className="text-lg font-semibold">Vinculações legais — execução {ano}</h2>
          <div className="grid gap-4 md:grid-cols-3">
            <Gauge label="MDE 25%"            executado={fundeb.pctMde}            minimo="25" baseLegal="CF/88, art. 212"  cumpre={fundeb.cumpreMde} />
            <Gauge label="Fundeb 70% pessoal" executado={fundeb.pctFundebPessoal}  minimo="70" baseLegal="Lei 14.113/2020" cumpre={fundeb.cumpreFundebPessoal} />
            <Gauge label="VAAT 15% capital"   executado={fundeb.pctVaatCapital}    minimo="15" baseLegal="Lei 14.113/2020" cumpre={fundeb.cumpreVaatCapital} />
          </div>
          <p className="text-xs text-slate-500">
            <Link href={`/transparencia/fundeb?ano=${ano}`} className="text-brand hover:underline">
              Ver receitas e despesas detalhadas →
            </Link>
          </p>
        </section>
      )}

      <section aria-labelledby="kpis" className="space-y-3">
        <h2 id="kpis" className="text-lg font-semibold">Resumo do exercício</h2>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card
            title="Notificações abertas"
            subtitle={`${criticas.length} crítica(s)`}
            variant={abertas.length === 0 ? 'success' : criticas.length > 0 ? 'danger' : 'warn'}
          >
            <StatNumber value={String(abertas.length)} />
          </Card>

          <Card title="Cálculos CAQ persistidos" subtitle="por escola/etapa">
            <StatNumber value={String(calculos?.length ?? 0)} />
          </Card>

          <Card title="Contratos firmados" subtitle="Lei 14.133/2021">
            <StatNumber value={fmt.money(totalContratado.toFixed(2))} />
            <p className="mt-0.5 text-xs text-slate-500">{(contratos ?? []).length} contrato(s)</p>
          </Card>

          <Card title="Publicações LRF 48-A" subtitle="snapshots arquivados">
            <StatNumber value={String((publicacoes ?? []).length)} />
          </Card>
        </div>
      </section>

      <section aria-labelledby="alertas" className="space-y-3">
        <header className="flex items-baseline justify-between">
          <h2 id="alertas" className="text-lg font-semibold">Alertas que exigem manifestação do conselho</h2>
          <Link href={`/transparencia/notificacoes?ano=${ano}`} className="text-sm text-brand hover:underline">
            Ver todas →
          </Link>
        </header>
        {recentNotificacoes.length === 0 ? (
          <p role="status" className="rounded border border-slate-200 bg-white p-4 text-sm text-slate-500">
            Nenhuma notificação no exercício.
          </p>
        ) : (
          <ul className="space-y-2">
            {recentNotificacoes.map((n) => (
              <li
                key={n.id}
                className={`rounded border-l-4 bg-white p-3 shadow-sm ${
                  n.severidade === 'critica' ? 'border-danger' :
                  n.severidade === 'alta'    ? 'border-warning' :
                  n.severidade === 'warn'    ? 'border-warning/60' :
                                                'border-slate-300'
                }`}
              >
                <div className="flex flex-wrap items-baseline gap-2 text-sm">
                  <strong>{n.titulo}</strong>
                  <span className="rounded bg-slate-100 px-2 py-0.5 text-xs uppercase text-slate-600">
                    {n.severidade}
                  </span>
                  <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
                    {n.status.replace('_', ' ')}
                  </span>
                  <span className="ml-auto text-xs text-slate-500">{fmt.date(n.createdAt)}</span>
                </div>
                <p className="mt-1 text-xs text-slate-600">{n.descricao}</p>
                {n.baseLegal && (
                  <p className="mt-1 text-xs text-slate-500"><strong>Base legal:</strong> {n.baseLegal}</p>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="publicacoes" className="space-y-3">
        <header className="flex items-baseline justify-between">
          <h2 id="publicacoes" className="text-lg font-semibold">Últimas publicações arquivadas (LRF 48-A)</h2>
          <Link href="/transparencia/publicacoes" className="text-sm text-brand hover:underline">
            Trilha completa →
          </Link>
        </header>
        {ultimasPublicacoes.length === 0 ? (
          <p role="status" className="rounded border border-slate-200 bg-white p-4 text-sm text-slate-500">
            Ainda não há publicações registradas.
          </p>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
            <table className="w-full text-sm">
              <caption className="sr-only">5 publicações mais recentes</caption>
              <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500">
                <tr>
                  <th scope="col" className="px-3 py-2 text-left">Tipo</th>
                  <th scope="col" className="px-3 py-2 text-left">Referência</th>
                  <th scope="col" className="px-3 py-2 text-left">Publicado em</th>
                  <th scope="col" className="px-3 py-2 text-left">Hash</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {ultimasPublicacoes.map((p) => (
                  <tr key={p.id}>
                    <td className="px-3 py-2">{p.tipo.replace(/_/g, ' ')}</td>
                    <td className="px-3 py-2 tabular-nums">{p.referencia || '—'}</td>
                    <td className="px-3 py-2 tabular-nums">{fmt.date(p.dataPublicacao)}</td>
                    <td className="px-3 py-2 font-mono text-xs">
                      <abbr title={p.conteudoHash} className="cursor-help no-underline">
                        {p.conteudoHash?.slice(0, 12) ?? '—'}…
                      </abbr>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section aria-labelledby="atribuicoes" className="rounded-lg border border-slate-200 bg-white p-6">
        <h2 id="atribuicoes" className="text-sm font-semibold uppercase tracking-wider text-slate-500">
          O que cabe a cada conselho
        </h2>
        <dl className="mt-3 grid gap-4 text-sm md:grid-cols-2">
          <div>
            <dt className="font-semibold text-slate-700">CACS-Fundeb (Lei 14.113/2020 art. 33-34)</dt>
            <dd className="mt-1 text-slate-600">
              Acompanha distribuição, transferência e aplicação dos recursos do Fundeb,
              examina prestações de contas, emite parecer e supervisiona o Censo Escolar
              (que alimenta o cálculo do VAAF/VAAT).
            </dd>
          </div>
          <div>
            <dt className="font-semibold text-slate-700">CME (LDB art. 11; PNE meta 19)</dt>
            <dd className="mt-1 text-slate-600">
              Função normativa, deliberativa e de controle social do sistema municipal de
              ensino. Acompanha o Plano Municipal de Educação, autoriza funcionamento de
              instituições e propõe diretrizes pedagógicas.
            </dd>
          </div>
        </dl>
      </section>
    </div>
  );
}
