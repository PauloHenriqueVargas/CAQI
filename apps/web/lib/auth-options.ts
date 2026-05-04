import type { NextAuthOptions } from 'next-auth';
import CredentialsProvider from 'next-auth/providers/credentials';

import { SERVICES } from './services';

/**
 * NextAuth v4 — Credentials Provider em 1 etapa com MFA opcional.
 *
 * Fluxo de authorize():
 *   1. Valida usuário/senha contra GET /api/v1/caqi/insumos (HTTP Basic)
 *   2. Consulta GET /api/v1/auth/mfa/status para o usuário
 *   3. Se MFA habilitado → exige `mfaCode` válido (POST /verify)
 *   4. Se MFA desabilitado → ignora `mfaCode` e autoriza
 *
 * Erros são genéricos para evitar enumeração; UI exibe campo MFA opcional
 * sempre, e a mensagem de erro orienta a tentar com o código se necessário.
 *
 * Em Fase 9.B-Gov.br: substituir por OAuth2 provider; backend passa a
 * validar JWT do Gov.br em vez de Basic.
 */
export const authOptions: NextAuthOptions = {
  providers: [
    CredentialsProvider({
      name: 'CAQi',
      credentials: {
        username: { label: 'Usuário', type: 'text' },
        password: { label: 'Senha',   type: 'password' },
        mfaCode:  { label: 'Código MFA (se habilitado)', type: 'text' },
      },
      async authorize(credentials) {
        if (!credentials?.username || !credentials?.password) return null;
        const auth = Buffer.from(`${credentials.username}:${credentials.password}`).toString('base64');
        const headers = { Authorization: `Basic ${auth}` };

        // 1. Valida usuário/senha
        try {
          const probe = await fetch(`${SERVICES.engine}/api/v1/caqi/insumos`, {
            headers,
            cache: 'no-store',
          });
          if (!probe.ok) return null;
        } catch {
          return null;
        }

        // 2. Consulta status do MFA
        let mfaEnabled = false;
        try {
          const status = await fetch(`${SERVICES.engine}/api/v1/auth/mfa/status`, {
            headers,
            cache: 'no-store',
          });
          if (status.ok) {
            const body = (await status.json()) as { enabled?: boolean };
            mfaEnabled = !!body.enabled;
          }
        } catch {
          // Se /status não responde, falha seguro (assume MFA habilitado)
          mfaEnabled = true;
        }

        // 3. Se MFA habilitado, exige código válido
        if (mfaEnabled) {
          const codigo = credentials.mfaCode?.trim();
          if (!codigo || !/^\d{6}$/.test(codigo)) return null;
          try {
            const verify = await fetch(`${SERVICES.engine}/api/v1/auth/mfa/verify`, {
              method: 'POST',
              headers: { ...headers, 'Content-Type': 'application/json' },
              body: JSON.stringify({ codigo }),
              cache: 'no-store',
            });
            if (!verify.ok) return null;
          } catch {
            return null;
          }
        }

        return {
          id: credentials.username,
          name: credentials.username,
          email: `${credentials.username}@caqi.local`,
          basicAuth: auth,
        };
      },
    }),
  ],
  session: { strategy: 'jwt', maxAge: 60 * 60 * 8 },
  pages: { signIn: '/login' },
  callbacks: {
    async jwt({ token, user }) {
      if (user && (user as { basicAuth?: string }).basicAuth) {
        token.basicAuth = (user as { basicAuth?: string }).basicAuth;
      }
      return token;
    },
    async session({ session, token }) {
      if (token.basicAuth) {
        (session as typeof session & { basicAuth?: string }).basicAuth = token.basicAuth as string;
      }
      return session;
    },
  },
};

declare module 'next-auth/jwt' {
  interface JWT {
    basicAuth?: string;
  }
}
