import Link from 'next/link';

import { fetchService, ApiError } from '@/lib/api-client';
import type { MfaStatusDto } from '@/lib/types';

import { MfaManagementUI } from './MfaManagementUI';

export const dynamic = 'force-dynamic';

export default async function MfaProfilePage() {
  let status: MfaStatusDto | null = null;
  let erroInicial: string | null = null;

  try {
    status = await fetchService<MfaStatusDto>('engine', '/api/v1/auth/mfa/status');
  } catch (e) {
    erroInicial = e instanceof ApiError ? `Backend ${e.status}: ${e.message}` : String(e);
  }

  return (
    <div className="max-w-2xl space-y-6">
      <Link href="/dashboard" className="text-sm text-brand hover:underline">← Dashboard</Link>

      <header>
        <h1 className="text-2xl font-bold">Autenticação por segundo fator (MFA)</h1>
        <p className="text-sm text-slate-500">
          Use um app autenticador (Google Authenticator, Aegis, 1Password, Authy) para
          adicionar uma camada extra de segurança. Padrão TOTP RFC 6238.
        </p>
      </header>

      {erroInicial && (
        <p role="alert" className="rounded border border-danger/30 bg-danger/5 p-3 text-sm text-danger">
          Não foi possível obter o status do MFA: {erroInicial}
        </p>
      )}

      {status && <MfaManagementUI initialStatus={status} />}
    </div>
  );
}
