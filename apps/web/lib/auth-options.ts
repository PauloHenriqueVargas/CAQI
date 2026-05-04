import type { NextAuthOptions } from 'next-auth';
import CredentialsProvider from 'next-auth/providers/credentials';

import { SERVICES } from './services';

/**
 * NextAuth v4 — Credentials Provider que valida usuário/senha contra o
 * caq-engine-svc (HTTP Basic Auth). A credencial Basic é guardada no JWT
 * e reutilizada pelo BFF quando proxia chamadas para os Spring services.
 *
 * Quando migrarmos para Gov.br OAuth2 (Fase 9), substituímos este provider
 * e o backend passa a validar JWT do Gov.br em vez de Basic Auth.
 */
export const authOptions: NextAuthOptions = {
  providers: [
    CredentialsProvider({
      name: 'CAQi',
      credentials: {
        username: { label: 'Usuário', type: 'text' },
        password: { label: 'Senha',   type: 'password' },
      },
      async authorize(credentials) {
        if (!credentials?.username || !credentials?.password) return null;
        const auth = Buffer.from(`${credentials.username}:${credentials.password}`).toString('base64');

        // Bate em /actuator/health (público) NÃO valida creds — usar um endpoint autenticado:
        const probe = `${SERVICES.engine}/api/v1/caqi/insumos`;
        try {
          const res = await fetch(probe, {
            headers: { Authorization: `Basic ${auth}` },
            cache: 'no-store',
          });
          if (!res.ok) return null;
        } catch {
          return null;
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
