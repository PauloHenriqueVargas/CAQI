'use client';

import { QRCodeSVG } from 'qrcode.react';
import { useState } from 'react';

import type {
  MfaBackupCodesResponseDto,
  MfaEnableResponseDto,
  MfaSetupResponseDto,
  MfaStatusDto,
} from '@/lib/types';

type Modo = 'idle' | 'configurando' | 'desativando' | 'regenerando' | 'mostrando_codes';

export function MfaManagementUI({ initialStatus }: { initialStatus: MfaStatusDto }) {
  const [status, setStatus] = useState<MfaStatusDto>(initialStatus);
  const [modo, setModo] = useState<Modo>('idle');
  const [setupData, setSetupData] = useState<MfaSetupResponseDto | null>(null);
  const [backupCodes, setBackupCodes] = useState<string[]>([]);
  const [codigo, setCodigo] = useState('');
  const [mensagem, setMensagem] = useState<{ tipo: 'sucesso' | 'erro'; texto: string } | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function chamarBackend(acao: string, body?: unknown) {
    setCarregando(true);
    setMensagem(null);
    try {
      const res = await fetch(`/api/mfa/${acao}`, {
        method: 'POST',
        headers: body ? { 'Content-Type': 'application/json' } : undefined,
        body: body ? JSON.stringify(body) : undefined,
      });
      const json = await res.json();
      if (!res.ok) {
        setMensagem({ tipo: 'erro', texto: json.erro ?? `Erro ${res.status}` });
        return null;
      }
      return json;
    } catch (e) {
      setMensagem({ tipo: 'erro', texto: e instanceof Error ? e.message : 'Erro desconhecido' });
      return null;
    } finally {
      setCarregando(false);
    }
  }

  async function iniciarSetup() {
    const r = (await chamarBackend('setup')) as MfaSetupResponseDto | null;
    if (r) {
      setSetupData(r);
      setModo('configurando');
      setCodigo('');
    }
  }

  async function confirmarAtivacao() {
    if (!/^\d{6}$/.test(codigo)) {
      setMensagem({ tipo: 'erro', texto: 'Código deve ter 6 dígitos.' });
      return;
    }
    const r = (await chamarBackend('enable', { codigo })) as MfaEnableResponseDto | null;
    if (r?.enabled) {
      setStatus({ enabled: true, username: r.username, backupCodesRemaining: r.backupCodes.length });
      setBackupCodes(r.backupCodes);
      setModo('mostrando_codes');
      setSetupData(null);
      setCodigo('');
      setMensagem({
        tipo: 'sucesso',
        texto: 'MFA habilitado. GUARDE OS BACKUP CODES ABAIXO — eles não serão exibidos novamente.',
      });
    }
  }

  async function desativarMfa() {
    if (!/^\d{6}$/.test(codigo)) {
      setMensagem({ tipo: 'erro', texto: 'Confirme com o código atual de 6 dígitos.' });
      return;
    }
    const r = (await chamarBackend('disable', { codigo })) as MfaStatusDto | null;
    if (r && !r.enabled) {
      setStatus(r);
      setModo('idle');
      setCodigo('');
      setBackupCodes([]);
      setMensagem({ tipo: 'sucesso', texto: 'MFA desativado.' });
    }
  }

  async function regenerarBackupCodes() {
    if (!/^\d{6}$/.test(codigo)) {
      setMensagem({ tipo: 'erro', texto: 'Digite o código TOTP atual de 6 dígitos.' });
      return;
    }
    const r = (await chamarBackend('regenerate-backup-codes', { codigo })) as MfaBackupCodesResponseDto | null;
    if (r && r.backupCodes.length > 0) {
      setBackupCodes(r.backupCodes);
      setStatus({ ...status, backupCodesRemaining: r.backupCodes.length });
      setModo('mostrando_codes');
      setCodigo('');
      setMensagem({
        tipo: 'sucesso',
        texto: 'Novos backup codes gerados. Os anteriores foram invalidados — guarde os novos.',
      });
    }
  }

  // ──────────────────────────── Render ────────────────────────────

  if (modo === 'mostrando_codes' && backupCodes.length > 0) {
    return (
      <section className="rounded-lg border-2 border-warning/60 bg-warning/5 p-6 shadow-sm">
        <Aviso m={mensagem} />
        <p className="text-sm font-semibold uppercase tracking-wider text-warning">
          ⚠ BACKUP CODES — GUARDE AGORA
        </p>
        <p className="mt-2 text-sm text-slate-700">
          Estes 8 códigos são <strong>one-time-use</strong> e <strong>não serão exibidos novamente</strong>.
          Use cada um apenas uma vez se você perder o app autenticador. Salve em local seguro
          (gerenciador de senhas, cofre físico).
        </p>

        <div className="mt-4 grid gap-2 sm:grid-cols-2 md:grid-cols-4">
          {backupCodes.map((c, i) => (
            <code
              key={c}
              className="rounded border border-warning/40 bg-white px-3 py-2 text-center font-mono text-sm tracking-widest"
            >
              {i + 1}. {c}
            </code>
          ))}
        </div>

        <div className="mt-5 flex flex-wrap gap-3">
          <button
            type="button"
            onClick={() => navigator.clipboard.writeText(backupCodes.join('\n'))}
            className="rounded border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
          >
            Copiar todos
          </button>
          <button
            type="button"
            onClick={() => {
              const blob = new Blob([backupCodes.join('\n')], { type: 'text/plain' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = `caqi-backup-codes-${status.username}.txt`;
              a.click();
              URL.revokeObjectURL(url);
            }}
            className="rounded border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
          >
            Baixar .txt
          </button>
          <button
            type="button"
            onClick={() => { setModo('idle'); setBackupCodes([]); setMensagem(null); }}
            className="ml-auto rounded bg-brand px-5 py-2 font-semibold text-brand-fg hover:bg-brand/90"
          >
            Já guardei — fechar
          </button>
        </div>
      </section>
    );
  }

  if (status.enabled && modo === 'idle') {
    return (
      <div className="space-y-4">
        <section className="rounded-lg border-2 border-success/40 bg-white p-6 shadow-sm">
          <Aviso m={mensagem} />
          <p className="text-sm font-semibold uppercase tracking-wider text-success">MFA HABILITADO</p>
          <p className="mt-1 text-sm text-slate-600">
            Usuário <strong>{status.username}</strong> protegido por TOTP.
          </p>
          <p className="mt-2 text-sm">
            Backup codes restantes:{' '}
            <strong className={status.backupCodesRemaining < 3 ? 'text-danger' : 'text-slate-700'}>
              {status.backupCodesRemaining} / 8
            </strong>
            {status.backupCodesRemaining < 3 && (
              <span className="ml-2 text-xs text-danger">— recomendado regenerar</span>
            )}
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => { setModo('regenerando'); setMensagem(null); setCodigo(''); }}
              className="rounded border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
            >
              Regenerar backup codes
            </button>
            <button
              type="button"
              onClick={() => { setModo('desativando'); setMensagem(null); setCodigo(''); }}
              className="rounded border border-danger/40 px-4 py-2 text-sm text-danger hover:bg-danger/5"
            >
              Desativar MFA
            </button>
          </div>
        </section>
      </div>
    );
  }

  if (modo === 'regenerando') {
    return (
      <section className="rounded-lg border-2 border-brand/30 bg-white p-6 shadow-sm">
        <Aviso m={mensagem} />
        <p className="text-sm font-semibold uppercase tracking-wider text-brand">REGENERAR BACKUP CODES</p>
        <p className="mt-1 text-sm text-slate-600">
          Os 8 codes atuais serão <strong>invalidados</strong>. Confirme com o código TOTP atual.
        </p>
        <div className="mt-4 flex items-center gap-3">
          <CodigoInput value={codigo} onChange={setCodigo} />
          <button
            type="button"
            disabled={carregando}
            onClick={regenerarBackupCodes}
            className="rounded bg-brand px-5 py-2 font-semibold text-brand-fg hover:bg-brand/90 disabled:opacity-60"
          >
            {carregando ? '...' : 'Regenerar'}
          </button>
          <button
            type="button"
            onClick={() => { setModo('idle'); setMensagem(null); }}
            className="rounded border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
          >
            Cancelar
          </button>
        </div>
      </section>
    );
  }

  if (modo === 'desativando') {
    return (
      <section className="rounded-lg border-2 border-warning/40 bg-white p-6 shadow-sm">
        <Aviso m={mensagem} />
        <p className="text-sm font-semibold uppercase tracking-wider text-warning">DESATIVAR MFA</p>
        <p className="mt-1 text-sm text-slate-600">
          Confirme com o código atual de 6 dígitos do app autenticador.
        </p>
        <div className="mt-4 flex items-center gap-3">
          <CodigoInput value={codigo} onChange={setCodigo} />
          <button
            type="button"
            disabled={carregando}
            onClick={desativarMfa}
            className="rounded bg-danger px-4 py-2 font-semibold text-white hover:bg-danger/90 disabled:opacity-60"
          >
            {carregando ? '...' : 'Confirmar desativação'}
          </button>
          <button
            type="button"
            onClick={() => { setModo('idle'); setMensagem(null); }}
            className="rounded border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
          >
            Cancelar
          </button>
        </div>
      </section>
    );
  }

  if (modo === 'configurando' && setupData) {
    return (
      <section className="rounded-lg border-2 border-brand/30 bg-white p-6 shadow-sm">
        <Aviso m={mensagem} />
        <p className="text-sm font-semibold uppercase tracking-wider text-brand">CONFIGURAR MFA</p>
        <ol className="mt-3 list-decimal space-y-2 pl-5 text-sm text-slate-700">
          <li>Abra seu app autenticador (Google Authenticator, Aegis, 1Password, Authy).</li>
          <li>Escaneie o QR abaixo OU cole a chave secreta manualmente.</li>
          <li>Digite o código de 6 dígitos que o app gerar para confirmar.</li>
          <li>Após confirmar, você receberá <strong>8 backup codes</strong> — guarde-os.</li>
        </ol>

        <div className="mt-5 flex flex-wrap items-start gap-6">
          <div className="rounded border border-slate-200 bg-white p-3">
            <QRCodeSVG value={setupData.otpauthUri} size={200} level="M" />
          </div>
          <div className="flex-1 space-y-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Chave (entrada manual)
              </p>
              <code className="mt-1 block break-all rounded bg-slate-100 p-2 font-mono text-xs">
                {setupData.secret}
              </code>
            </div>
            <details>
              <summary className="cursor-pointer text-xs text-slate-500">Mostrar URI otpauth</summary>
              <code className="mt-1 block break-all rounded bg-slate-100 p-2 font-mono text-xs">
                {setupData.otpauthUri}
              </code>
            </details>
          </div>
        </div>

        <div className="mt-6 flex items-center gap-3">
          <CodigoInput value={codigo} onChange={setCodigo} />
          <button
            type="button"
            disabled={carregando}
            onClick={confirmarAtivacao}
            className="rounded bg-brand px-5 py-2 font-semibold text-brand-fg hover:bg-brand/90 disabled:opacity-60"
          >
            {carregando ? '...' : 'Ativar'}
          </button>
          <button
            type="button"
            onClick={() => { setModo('idle'); setSetupData(null); setMensagem(null); }}
            className="rounded border border-slate-300 px-4 py-2 text-sm hover:bg-slate-50"
          >
            Cancelar
          </button>
        </div>
      </section>
    );
  }

  // status.enabled === false && modo === 'idle'
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <Aviso m={mensagem} />
      <p className="text-sm font-semibold uppercase tracking-wider text-slate-500">MFA NÃO HABILITADO</p>
      <p className="mt-1 text-sm text-slate-600">
        Recomendamos fortemente habilitar para usuários ADMIN. Após habilitado, o login
        passa a exigir o código de 6 dígitos do app autenticador (ou um backup code).
      </p>
      <button
        type="button"
        disabled={carregando}
        onClick={iniciarSetup}
        className="mt-4 rounded bg-brand px-5 py-2 font-semibold text-brand-fg hover:bg-brand/90 disabled:opacity-60"
      >
        {carregando ? '...' : 'Configurar MFA'}
      </button>
    </section>
  );
}

function CodigoInput({ value, onChange }: { value: string; onChange: (s: string) => void }) {
  return (
    <input
      type="text"
      inputMode="numeric"
      pattern="\d{6}"
      maxLength={6}
      autoComplete="one-time-code"
      placeholder="000000"
      value={value}
      onChange={(e) => onChange(e.target.value.replace(/\D/g, ''))}
      className="w-32 rounded border border-slate-300 px-3 py-2 text-center font-mono tracking-widest focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
    />
  );
}

function Aviso({ m }: { m: { tipo: 'sucesso' | 'erro'; texto: string } | null }) {
  if (!m) return null;
  const cls = m.tipo === 'sucesso'
    ? 'border-success/40 bg-success/5 text-success'
    : 'border-danger/40 bg-danger/5 text-danger';
  return (
    <p role="alert" className={`mb-4 rounded border ${cls} p-3 text-sm`}>
      {m.texto}
    </p>
  );
}
