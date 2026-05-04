'use client';

import { useActionState } from 'react';

import { ErrorBanner, Field, FormCard } from '@/components/Form';
import { SubmitButton } from '@/components/SubmitButton';

import { criarFornecedor, type EstadoFormFornecedor } from './actions';

export function FornecedorForm() {
  const [estado, formAction] = useActionState<EstadoFormFornecedor, FormData>(criarFornecedor, {});

  return (
    <form action={formAction}>
      <FormCard title="Dados do fornecedor (PJ)">
        <Field
          label="CNPJ"
          name="cnpj"
          required
          placeholder="00.000.000/0001-00"
          helper="Formato livre — backend valida unicidade."
        />
        <Field label="Nome / Razão social" name="nome" required placeholder="Empresa LIMPA Ltda." />
        <Field label="Município" name="municipio" placeholder="Palmas/TO" />

        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            name="optanteSimples"
            className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand"
          />
          <span className="text-sm">
            <strong>Optante do Simples Nacional</strong> (LC 123/2006) — afeta retenção tributária:
            apenas ISS é retido na fonte; IRRF/INSS/PIS/COFINS/CSLL via DAS unificado.
          </span>
        </label>

        <ErrorBanner message={estado.erro} />
        <div className="flex items-center justify-end pt-2">
          <SubmitButton>Criar fornecedor</SubmitButton>
        </div>
      </FormCard>
    </form>
  );
}
