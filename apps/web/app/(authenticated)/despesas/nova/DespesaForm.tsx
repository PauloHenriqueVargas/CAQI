'use client';

import { useActionState } from 'react';

import { ErrorBanner, Field, FormCard, Select } from '@/components/Form';
import { SubmitButton } from '@/components/SubmitButton';
import { SIOPE_GRUPOS } from '@/lib/types';
import type { FonteRecursoDto } from '@/lib/types';

import { criarDespesa, type EstadoFormDespesa } from './actions';

export function DespesaForm({ fontes }: { fontes: FonteRecursoDto[] }) {
  const [estado, formAction] = useActionState<EstadoFormDespesa, FormData>(criarDespesa, {});

  const competenciaPadrao = (() => {
    const d = new Date();
    return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}`;
  })();

  return (
    <form action={formAction}>
      <FormCard
        title="Lançamento de despesa"
        subtitle="Natureza começando com 3.1 = pessoal (entra em Fundeb 70%); 4.x = capital (entra em VAAT 15% se fonte=VAAT)."
      >
        <div className="grid gap-4 md:grid-cols-2">
          <Field
            label="Competência (YYYYMM)"
            name="competencia"
            required
            pattern="\d{6}"
            defaultValue={competenciaPadrao}
            placeholder="202506"
          />
          <Field
            label="Valor (R$)"
            name="valor"
            type="number"
            step="0.01"
            min={0.01}
            required
            placeholder="50000.00"
          />
        </div>
        <Field
          label="Natureza (PCASP)"
          name="natureza"
          required
          placeholder="3.1.90.11"
          helper="3.1.x = pessoal, 4.4.x = capital, 3.3.x = consumo/serviços."
        />
        <Select
          label="Fonte de recurso"
          name="fonteRecursoId"
          options={[
            { value: '', label: '— Sem fonte (não recomendado para Fundeb) —' },
            ...fontes.map((f) => ({
              value: String(f.id),
              label: `${f.tipo}${f.descricao ? ` · ${f.descricao}` : ''}`,
            })),
          ]}
        />
        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Grupo SIOPE"
            name="siopeGrupo"
            options={[
              { value: '', label: '— Não classificada —' },
              ...SIOPE_GRUPOS.map((g) => ({ value: g, label: g })),
            ]}
            helper="Despesas MDE entram no relatório FNDE."
          />
          <Field label="PCASP (opcional)" name="pcasp" placeholder="3.1.90.11.00" />
        </div>

        {estado.bloqueado && (
          <div role="alert" className="rounded border-2 border-danger bg-danger/5 p-4">
            <p className="text-sm font-semibold uppercase tracking-wider text-danger">
              ⛔ EMPENHO BLOQUEADO PELO VALIDADOR
            </p>
            <p className="mt-2 text-sm text-slate-700">{estado.bloqueado}</p>
            <p className="mt-2 text-xs text-slate-500">
              A flag <code>caqi.compliance.bloquear-empenhos-violadores=true</code> está
              ativa. A despesa derrubaria uma vinculação legal de "cumpre" para
              "não-cumpre". Reclassifique fonte/natureza/siope_grupo OU compense com
              despesa de pessoal Fundeb antes de tentar novamente.
            </p>
          </div>
        )}
        <ErrorBanner message={estado.erro} />
        <div className="flex items-center justify-end pt-2">
          <SubmitButton>Lançar despesa</SubmitButton>
        </div>
      </FormCard>
    </form>
  );
}
