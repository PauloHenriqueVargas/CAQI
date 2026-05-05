import type { Metadata } from 'next';

import { Card, StatNumber } from '@/components/Card';
import { Gauge } from '@/components/Gauge';
import { fetchPublic } from '@/lib/api-public';
import type { ExecucaoFundebDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Execução Fundeb / MDE / VAAT',
  description:
    'Acompanhe a execução das vinculações constitucionais e legais da educação municipal: ' +
    'MDE 25% (CF/88 art. 212), Fundeb 70% pessoal e VAAT 15% capital (Lei 14.113/2020).',
};

export default async function FundebPublico({
  searchParams,
}: {
  searchParams: { ano?: string };
}) {
  const ano = searchParams.ano ? parseInt(searchParams.ano, 10) : new Date().getFullYear();
  const exec = await fetchPublic<ExecucaoFundebDto>(
    'financeiro',
    `/api/public/transparencia/fundeb-execucao?ano=${ano}`,
  );

  if (!exec) {
    return (
      <div className="space-y-4">
        <h1 className="text-2xl font-bold">Execução Fundeb · {ano}</h1>
        <BannerSemDados />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <header>
        <p className="text-xs font-semibold uppercase tracking-widest text-brand">
          Vinculações legais · Exercício {ano}
        </p>
        <h1 className="text-2xl font-bold">Execução Fundeb / MDE / VAAT</h1>
        <p className="mt-1 max-w-3xl text-sm text-slate-600">
          Indicadores de cumprimento dos mínimos legais aplicáveis à educação municipal.
          Apuração anual; valores podem ser revistos até o fechamento do exercício.
        </p>
      </header>

      <section aria-labelledby="vinculacoes" className="space-y-3">
        <h2 id="vinculacoes" className="text-lg font-semibold">Vinculações</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <Gauge label="MDE 25%"             executado={exec.pctMde}             minimo="25" baseLegal="CF/88, art. 212"     cumpre={exec.cumpreMde} />
          <Gauge label="Fundeb 70% pessoal"  executado={exec.pctFundebPessoal}   minimo="70" baseLegal="Lei 14.113/2020"     cumpre={exec.cumpreFundebPessoal} />
          <Gauge label="VAAT 15% capital"    executado={exec.pctVaatCapital}     minimo="15" baseLegal="Lei 14.113/2020"     cumpre={exec.cumpreVaatCapital} />
        </div>
      </section>

      <section aria-labelledby="receitas" className="space-y-3">
        <h2 id="receitas" className="text-lg font-semibold">Receitas</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <Card title="Impostos + transferências" subtitle="Base do MDE 25%">
            <StatNumber value={fmt.money(exec.receitasImpostos)} />
          </Card>
          <Card title="Fundeb (VAAF + VAAT + VAAR)" subtitle="Base do Fundeb 70%">
            <StatNumber value={fmt.money(exec.receitasFundeb)} />
          </Card>
          <Card title="Complementação VAAT" subtitle="Base do VAAT 15% capital">
            <StatNumber value={fmt.money(exec.receitasVaat)} />
          </Card>
        </div>
      </section>

      <section aria-labelledby="despesas" className="space-y-3">
        <h2 id="despesas" className="text-lg font-semibold">Despesas executadas</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <Card title="Despesas MDE">
            <StatNumber value={fmt.money(exec.despesasMde)} />
          </Card>
          <Card title="Pessoal Fundeb">
            <StatNumber value={fmt.money(exec.despesasPessoalFundeb)} />
          </Card>
          <Card title="Capital VAAT">
            <StatNumber value={fmt.money(exec.despesasCapitalVaat)} />
          </Card>
        </div>
      </section>

      <section aria-labelledby="legenda" className="rounded-lg border border-slate-200 bg-white p-6">
        <h2 id="legenda" className="text-sm font-semibold uppercase tracking-wider text-slate-500">
          Como interpretar
        </h2>
        <dl className="mt-3 space-y-2 text-sm text-slate-700">
          <div>
            <dt className="font-semibold">MDE 25%</dt>
            <dd className="text-slate-600">
              Percentual da receita de impostos e transferências aplicado em manutenção e
              desenvolvimento do ensino. Mínimo constitucional (CF/88 art. 212).
            </dd>
          </div>
          <div>
            <dt className="font-semibold">Fundeb 70% pessoal</dt>
            <dd className="text-slate-600">
              Percentual dos recursos do Fundeb destinado à remuneração dos profissionais da
              educação básica em efetivo exercício (Lei 14.113/2020).
            </dd>
          </div>
          <div>
            <dt className="font-semibold">VAAT 15% capital</dt>
            <dd className="text-slate-600">
              Percentual da complementação-VAAT aplicado em despesas de capital (investimentos /
              inversões financeiras — natureza 4.x do PCASP).
            </dd>
          </div>
        </dl>
      </section>
    </div>
  );
}

function BannerSemDados() {
  return (
    <div role="status" className="rounded border border-slate-200 bg-white p-6 text-sm text-slate-600">
      <p>
        Os dados de execução Fundeb/MDE/VAAT deste exercício ainda não estão disponíveis
        publicamente. Tente novamente em alguns minutos. Se persistir, contate o SIC do município.
      </p>
    </div>
  );
}
