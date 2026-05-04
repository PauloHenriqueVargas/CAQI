'use client';

import { useActionState } from 'react';

import { ErrorBanner, Field, FormCard, Select } from '@/components/Form';
import { SubmitButton } from '@/components/SubmitButton';
import { ORIGENS_RECEITA } from '@/lib/types';

import { criarReceita, type EstadoFormReceita } from './actions';

export function ReceitaForm() {
  const [estado, formAction] = useActionState<EstadoFormReceita, FormData>(criarReceita, {});

  const competenciaPadrao = (() => {
    const d = new Date();
    return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}`;
  })();

  return (
    <form action={formAction}>
      <FormCard
        title="Lançamento de receita"
        subtitle="Origem afeta a base de cálculo: impostos+transferencias entram em MDE 25%; Fundeb_VAAF/VAAT/VAAR entram em Fundeb 70%; só VAAT entra em VAAT 15%."
      >
        <div className="grid gap-4 md:grid-cols-2">
          <Field
            label="Competência (YYYYMM)"
            name="competencia"
            required
            pattern="\d{6}"
            placeholder="202506"
            defaultValue={competenciaPadrao}
          />
          <Field
            label="Valor (R$)"
            name="valor"
            type="number"
            step="0.01"
            min={0.01}
            required
            placeholder="100000.00"
          />
        </div>
        <Select
          label="Origem"
          name="origem"
          required
          options={[
            { value: '', label: '— Selecione —' },
            ...ORIGENS_RECEITA.map((o) => ({ value: o, label: o })),
          ]}
        />
        <Field
          label="PCASP (opcional)"
          name="pcasp"
          placeholder="1.7.1.0.00.00"
          helper="Código contábil — recomendado para envio SIOPE."
        />
        <ErrorBanner message={estado.erro} />
        <div className="flex items-center justify-end pt-2">
          <SubmitButton>Lançar receita</SubmitButton>
        </div>
      </FormCard>
    </form>
  );
}
