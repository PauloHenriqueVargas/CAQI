'use client';

import { signIn } from 'next-auth/react';
import { useRouter, useSearchParams } from 'next/navigation';
import { FormEvent, useState } from 'react';

export default function LoginPage() {
  const router = useRouter();
  const params = useSearchParams();
  const callbackUrl = params.get('callbackUrl') ?? '/dashboard';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [mfaCode, setMfaCode] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    const res = await signIn('credentials', {
      username,
      password,
      mfaCode: mfaCode.trim() || undefined,
      redirect: false,
    });
    setCarregando(false);
    if (res?.error) {
      setErro('Usuário, senha ou código MFA inválidos.');
      return;
    }
    router.push(callbackUrl);
    router.refresh();
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-slate-100 px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-8 shadow"
      >
        <h1 className="text-xl font-bold text-brand">Sistema CAQ/CAQi</h1>
        <p className="mt-1 text-sm text-slate-500">
          {process.env.CAQI_MUNICIPIO_NOME ?? 'Município Exemplo'}
        </p>

        <div className="mt-6 space-y-4">
          <label className="block">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Usuário</span>
            <input
              type="text"
              autoComplete="username"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
            />
          </label>

          <label className="block">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Senha</span>
            <input
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
            />
          </label>

          <label className="block">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Código MFA <span className="font-normal text-slate-400">(se habilitado)</span>
            </span>
            <input
              type="text"
              inputMode="numeric"
              pattern="\d{6}"
              maxLength={6}
              autoComplete="one-time-code"
              placeholder="6 dígitos"
              value={mfaCode}
              onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, ''))}
              className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 font-mono tracking-widest focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
            />
          </label>

          {erro && (
            <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
              {erro}
            </p>
          )}

          <button
            type="submit"
            disabled={carregando}
            className="w-full rounded bg-brand px-4 py-2 font-semibold text-brand-fg hover:bg-brand/90 disabled:opacity-60"
          >
            {carregando ? 'Entrando…' : 'Entrar'}
          </button>
        </div>

        <p className="mt-6 text-xs text-slate-400">
          Em produção: login via Gov.br OAuth2 (Fase 9.B). MVP usa HTTP Basic do backend.
        </p>
      </form>
    </main>
  );
}
