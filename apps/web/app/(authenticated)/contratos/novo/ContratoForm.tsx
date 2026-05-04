'use client';

import { useActionState } from 'react';

import { ErrorBanner, Field, FormCard, Select } from '@/components/Form';
import { SubmitButton } from '@/components/SubmitButton';
import { MODALIDADES_LEI_14133 } from '@/lib/types';
import type { FornecedorDto } from '@/lib/types';

import { criarContrato, type EstadoFormContrato } from './actions';

export function ContratoForm({ fornecedores }: { fornecedores: FornecedorDto[] }) {
  const [estado, formAction] = useActionState<EstadoFormContrato, FormData>(criarContrato, {});

  return (
    <form action={formAction} className="space-y-4">
      <FormCard title="Dados do contrato" subtitle="Modalidades conforme Lei 14.133/2021">
        <Select
          label="Fornecedor"
          name="fornecedorId"
          required
          options={[
            { value: '', label: '— Selecione —' },
            ...fornecedores.map((f) => ({
              value: String(f.id),
              label: `${f.nome} · CNPJ ${f.cnpj}${f.optanteSimples ? ' · Simples' : ''}`,
            })),
          ]}
          helper={
            fornecedores.length === 0
              ? 'Nenhum fornecedor cadastrado — cadastre via POST /api/v1/financeiro/fornecedores antes.'
              : undefined
          }
        />
        <Field
          label="Objeto"
          name="objeto"
          required
          placeholder="Ex.: Limpeza e conservação das escolas Municipais — exercício 2026"
        />
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="Data de assinatura" name="dataAssinatura" type="date" required />
          <Field
            label="Valor global (R$)"
            name="valorGlobal"
            type="number"
            step="0.01"
            min={0}
            required
            placeholder="120000.00"
          />
        </div>
        <Select
          label="Modalidade"
          name="modalidade"
          required
          options={[
            { value: '', label: '— Selecione —' },
            ...MODALIDADES_LEI_14133.map((m) => ({
              value: m,
              label: m.replaceAll('_', ' '),
            })),
          ]}
        />
        <Field
          label="ID PNCP (opcional)"
          name="pncpId"
          placeholder="Identificador no Portal Nacional de Contratações Públicas"
          helper="Preencher após publicação no PNCP."
        />
        <ErrorBanner message={estado.erro} />
        <div className="flex items-center justify-end pt-2">
          <SubmitButton>Criar contrato</SubmitButton>
        </div>
      </FormCard>
    </form>
  );
}
