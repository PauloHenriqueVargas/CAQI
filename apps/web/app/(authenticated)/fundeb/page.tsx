import { Card, StatNumber } from '@/components/Card';
import { Gauge } from '@/components/Gauge';
import { fetchService } from '@/lib/api-client';
import type { ExecucaoFundebDto } from '@/lib/types';
import { fmt } from '@/lib/types';

export const dynamic = 'force-dynamic';

export default async function FundebPage({
  searchParams,
}: {
  searchParams: { ano?: string };
}) {
  const ano = searchParams.ano ? parseInt(searchParams.ano, 10) : new Date().getFullYear();
  const exec = await fetchService<ExecucaoFundebDto>(
    'financeiro',
    `/api/v1/fundeb/execucao?ano=${ano}`,
  );

  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-2xl font-bold">Fundeb / MDE / VAAT · Exercício {ano}</h1>
        <p className="text-sm text-slate-500">
          Execução das vinculações constitucionais e legais, com base nos lançamentos do exercício.
        </p>
      </header>

      <section className="grid gap-4 md:grid-cols-3">
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
      </section>

      <section aria-labelledby="receitas">
        <h2 id="receitas" className="mb-3 text-lg font-semibold">Receitas</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <Card title="Receitas de impostos + transferências" subtitle="Base do MDE 25%">
            <StatNumber value={fmt.money(exec.receitasImpostos)} />
          </Card>
          <Card title="Receitas Fundeb (VAAF + VAAT + VAAR)" subtitle="Base do Fundeb 70%">
            <StatNumber value={fmt.money(exec.receitasFundeb)} />
          </Card>
          <Card title="Complementação VAAT" subtitle="Base do VAAT 15% capital">
            <StatNumber value={fmt.money(exec.receitasVaat)} />
          </Card>
        </div>
      </section>

      <section aria-labelledby="despesas">
        <h2 id="despesas" className="mb-3 text-lg font-semibold">Despesas executadas</h2>
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

      <section className="rounded border border-slate-200 bg-white p-6">
        <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500">
          Como interpretar
        </h3>
        <ul className="mt-3 space-y-2 text-sm text-slate-600">
          <li>
            <strong>MDE 25%</strong> — receitas de impostos e transferências aplicadas em manutenção
            e desenvolvimento do ensino. Mínimo constitucional. Apuração anual.
          </li>
          <li>
            <strong>Fundeb 70%</strong> — recursos do Fundeb destinados à remuneração dos profissionais
            da educação básica em efetivo exercício. Apuração anual.
          </li>
          <li>
            <strong>VAAT 15%</strong> — complementação-VAAT aplicada em despesas de capital
            (4.x — investimentos / inversões financeiras). Apuração anual.
          </li>
        </ul>
      </section>
    </div>
  );
}
