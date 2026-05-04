'use client';

import { useActionState } from 'react';

import { ErrorBanner, Field, FormCard } from '@/components/Form';
import { SubmitButton } from '@/components/SubmitButton';

import { criarCalculo, type EstadoFormCalculo } from './actions';

const ANO_PADRAO = new Date().getFullYear();

export function CalculoForm() {
  const [estado, formAction] = useActionState<EstadoFormCalculo, FormData>(criarCalculo, {});

  return (
    <form action={formAction}>
      <FormCard
        title="Parâmetros do cálculo"
        subtitle="Use IDs de escola conforme o seed (ex.: 1, 2, 3) e códigos de etapa em maiúsculo (PRE, EF1, EF2, CRECHE, EJA, ESP)."
      >
        <Field
          label="Ano de referência"
          name="ano"
          type="number"
          required
          defaultValue={ANO_PADRAO}
          min={2020}
          max={2030}
          helper="Define a vigência usada para parâmetros, custos e índices."
        />
        <Field
          label="Escolas (IDs separados por vírgula)"
          name="escolas"
          required
          defaultValue="1"
          placeholder="1, 2, 3"
        />
        <Field
          label="Etapas (códigos separados por vírgula)"
          name="etapas"
          required
          defaultValue="EF1"
          placeholder="EF1, EF2, PRE"
        />
        <ErrorBanner message={estado.erro} />
        <div className="flex items-center justify-end gap-3 pt-2">
          <SubmitButton>Executar cálculo</SubmitButton>
        </div>
      </FormCard>
    </form>
  );
}
