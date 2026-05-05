import Link from 'next/link';

import { Card, StatNumber } from '@/components/Card';
import { fetchPublic } from '@/lib/api-public';
import type {
  CalculoCaqResumoDto,
  ContratoDto,
  ExecucaoFundebDto,
  NotificacaoDto,
} from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function TransparenciaHome() {
  const ano = new Date().getFullYear();

  const [fundeb, calculos, contratos, notificacoes] = await Promise.all([
    fetchPublic<ExecucaoFundebDto>('financeiro', `/api/public/transparencia/fundeb-execucao?ano=${ano}`),
    fetchPublic<CalculoCaqResumoDto[]>('engine', '/api/public/transparencia/calculos'),
    fetchPublic<ContratoDto[]>('financeiro', '/api/public/transparencia/contratos'),
    fetchPublic<NotificacaoDto[]>('compliance', `/api/public/transparencia/notificacoes?ano=${ano}`),
  ]);

  const totalContratado = (contratos ?? []).reduce(
    (acc, c) => acc + parseFloat(c.valorGlobal || '0'),
    0,
  );
  const abertas = (notificacoes ?? []).filter((n) => n.status === 'aberta').length;

  const cumpreTudo = fundeb
    ? fundeb.cumpreMde && fundeb.cumpreFundebPessoal && fundeb.cumpreVaatCapital
    : null;

  return (
    <div className="space-y-10">
      <header className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Portal público de educação · {ano}
        </p>
        <h1 className="text-3xl font-bold tracking-tight">Transparência da educação municipal</h1>
        <p className="max-w-3xl text-sm text-slate-600">
          Execução do Fundeb e do MDE, cálculo do <strong>Custo Aluno Qualidade (CAQ/CAQi)</strong>{' '}
          escola por escola, contratos firmados, despesas executadas e notificações de compliance —
          atualizado conforme execução orçamentária. Publicação ativa em conformidade com a Lei de
          Acesso à Informação (Lei 12.527/2011) e o art. 48-A da LRF.
        </p>
      </header>

      <section aria-labelledby="resumo" className="space-y-4">
        <h2 id="resumo" className="text-lg font-semibold">Resumo do exercício</h2>

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card
            title="Vinculações legais"
            subtitle={fundeb ? 'MDE 25% · Fundeb 70% · VAAT 15%' : 'sem dados'}
            variant={cumpreTudo === null ? 'default' : cumpreTudo ? 'success' : 'danger'}
          >
            <StatNumber
              value={cumpreTudo === null ? '—' : cumpreTudo ? 'Conforme' : 'Atenção'}
            />
            <p className="mt-1 text-xs text-slate-500">
              <Link href="/transparencia/fundeb" className="text-brand hover:underline">
                Ver detalhes →
              </Link>
            </p>
          </Card>

          <Card title="Cálculos CAQ" subtitle="por escola/etapa">
            <StatNumber value={String(calculos?.length ?? 0)} />
            <p className="mt-1 text-xs text-slate-500">
              <Link href="/transparencia/calculos" className="text-brand hover:underline">
                Ver lista →
              </Link>
            </p>
          </Card>

          <Card title="Contratos firmados" subtitle="Lei 14.133/2021">
            <StatNumber value={fmt.money(totalContratado.toFixed(2))} />
            <p className="mt-1 text-xs text-slate-500">
              {(contratos ?? []).length} contrato(s) ·{' '}
              <Link href="/transparencia/contratos" className="text-brand hover:underline">
                ver todos →
              </Link>
            </p>
          </Card>

          <Card
            title="Notificações abertas"
            subtitle="compliance MDE/Fundeb/VAAT"
            variant={abertas > 0 ? 'warn' : 'default'}
          >
            <StatNumber value={String(abertas)} />
            <p className="mt-1 text-xs text-slate-500">
              <Link href="/transparencia/notificacoes" className="text-brand hover:underline">
                Ver histórico →
              </Link>
            </p>
          </Card>
        </div>
      </section>

      <section aria-labelledby="o-que-publicamos" className="rounded-lg border border-slate-200 bg-white p-6">
        <h2 id="o-que-publicamos" className="text-lg font-semibold">O que esta página publica</h2>
        <ul className="mt-3 grid gap-3 text-sm text-slate-700 md:grid-cols-2">
          <li>
            <strong>Execução Fundeb / MDE / VAAT</strong> — receitas e despesas vinculadas, com
            indicador de cumprimento dos mínimos legais (CF/88 art. 212; Lei 14.113/2020).
          </li>
          <li>
            <strong>Cálculos CAQ/CAQi</strong> — valor por aluno-ano, escola por escola, com a
            memória item-a-item dos insumos (PNE meta 20.7).
          </li>
          <li>
            <strong>Contratos</strong> — modalidade, fornecedor, valor global, data de assinatura
            (Lei 14.133/2021 + LRF art. 48 §1°).
          </li>
          <li>
            <strong>Despesas</strong> — competência, natureza PCASP, fonte e valor agregado por
            ano (sem dados pessoais — LGPD art. 11).
          </li>
          <li>
            <strong>Notificações de compliance</strong> — alertas automáticos quando uma
            vinculação legal não é cumprida, com base legal e severidade.
          </li>
          <li>
            <strong>Não publicamos</strong> — folha individual, CPF/dados pessoais de servidor ou
            aluno, dados sensíveis. Pedidos via SIC do município (LAI).
          </li>
        </ul>
      </section>
    </div>
  );
}
