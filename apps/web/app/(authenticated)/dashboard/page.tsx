import { Card, StatNumber } from '@/components/Card';
import { Gauge } from '@/components/Gauge';
import { fetchService, ApiError } from '@/lib/api-client';
import type { CalculoCaqResumoDto, ExecucaoFundebDto, NotificacaoDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function DashboardPage() {
  const ano = new Date().getFullYear();
  let exec: ExecucaoFundebDto | null = null;
  let calculos: CalculoCaqResumoDto[] = [];
  let notificacoesAbertas: NotificacaoDto[] = [];
  const erros: string[] = [];

  try {
    exec = await fetchService<ExecucaoFundebDto>('financeiro', `/api/v1/fundeb/execucao?ano=${ano}`);
  } catch (e) {
    erros.push(`Fundeb: ${e instanceof ApiError ? e.message : String(e)}`);
  }
  try {
    calculos = await fetchService<CalculoCaqResumoDto[]>('engine', '/api/v1/caqi/calculos');
  } catch (e) {
    erros.push(`Cálculos: ${e instanceof ApiError ? e.message : String(e)}`);
  }
  try {
    notificacoesAbertas = await fetchService<NotificacaoDto[]>(
      'compliance',
      '/api/v1/compliance/notificacoes?status=aberta',
    );
  } catch (e) {
    erros.push(`Notificações: ${e instanceof ApiError ? e.message : String(e)}`);
  }

  // Métricas derivadas
  const calculosDoAno = calculos.filter((c) => c.ano === ano);
  const caqiMedio = mediaPondMoney(calculosDoAno.map((c) => c.valorCaqiAlunoAno));
  const gapMedio = mediaPondMoney(calculosDoAno.map((c) => c.gapAdequado));

  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-2xl font-bold">Painel executivo · Exercício {ano}</h1>
        <p className="text-sm text-slate-500">
          Visão consolidada das vinculações constitucionais/legais e do Custo Aluno Qualidade.
        </p>
      </header>

      {erros.length > 0 && (
        <div className="rounded border border-warning/30 bg-warning/10 p-4 text-sm">
          <p className="font-semibold text-warning">Atenção — alguns dados não puderam ser carregados:</p>
          <ul className="mt-1 list-disc pl-5">
            {erros.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        </div>
      )}

      <section aria-labelledby="kpis">
        <h2 id="kpis" className="sr-only">KPIs</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <Card title="CAQi médio" subtitle="R$/aluno/ano (média ponderada)">
            <StatNumber value={fmt.money(caqiMedio)} />
          </Card>
          <Card title="Gap CAQ adequado" subtitle="quanto falta por aluno/ano">
            <StatNumber value={fmt.money(gapMedio)} />
          </Card>
          <Card
            title="Notificações abertas"
            subtitle="violações pendentes de tratamento"
            variant={notificacoesAbertas.length > 0 ? 'danger' : 'success'}
          >
            <StatNumber value={String(notificacoesAbertas.length)} />
          </Card>
        </div>
      </section>

      {exec && (
        <section aria-labelledby="vinculacoes">
          <h2 id="vinculacoes" className="mb-3 text-lg font-semibold">Vinculações legais</h2>
          <div className="grid gap-4 md:grid-cols-3">
            <Gauge
              label="MDE 25%"
              executado={exec.pctMde}
              minimo="25"
              baseLegal="CF/88, art. 212"
              cumpre={exec.cumpreMde}
            />
            <Gauge
              label="Fundeb 70% pessoal"
              executado={exec.pctFundebPessoal}
              minimo="70"
              baseLegal="Lei 14.113/2020"
              cumpre={exec.cumpreFundebPessoal}
            />
            <Gauge
              label="VAAT 15% capital"
              executado={exec.pctVaatCapital}
              minimo="15"
              baseLegal="Lei 14.113/2020"
              cumpre={exec.cumpreVaatCapital}
            />
          </div>
        </section>
      )}

      {notificacoesAbertas.length > 0 && (
        <section aria-labelledby="alertas">
          <h2 id="alertas" className="mb-3 text-lg font-semibold">Alertas em aberto</h2>
          <ul className="space-y-2">
            {notificacoesAbertas.slice(0, 5).map((n) => (
              <li
                key={n.id}
                className={`rounded border-l-4 bg-white p-4 shadow-sm ${
                  n.severidade === 'critica' ? 'border-danger' :
                  n.severidade === 'alta'    ? 'border-warning' :
                                               'border-slate-300'
                }`}
              >
                <p className="font-semibold">{n.titulo}</p>
                <p className="text-sm text-slate-600">{n.descricao}</p>
                {n.baseLegal && <p className="mt-1 text-xs text-slate-400">{n.baseLegal}</p>}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

function mediaPondMoney(valores: string[]): string {
  if (valores.length === 0) return '0';
  const soma = valores.reduce((a, v) => a + parseFloat(v), 0);
  return (soma / valores.length).toFixed(2);
}
