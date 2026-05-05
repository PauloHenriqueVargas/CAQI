/**
 * Mock backend HTTP — substitui os 4 microserviços Spring durante os
 * testes E2E. Sem dependências externas (apenas Node.js stdlib).
 *
 * Roda 4 servidores em portas 9991-9994 simulando engine, financeiro,
 * escolar, compliance. Responde com fixtures JSON para os endpoints que
 * as páginas Next.js consomem via Server Components / Server Actions.
 *
 * Iniciado pelo Playwright como webServer paralelo ao `next start`.
 * Encerra ao receber SIGTERM/SIGINT.
 */

import http from 'node:http';
import { URL } from 'node:url';

// ── Fixtures ────────────────────────────────────────────────────────
const fundebFixture = {
  ano: new Date().getFullYear(),
  receitasImpostos: '180000.00',
  receitasFundeb: '280000.00',
  receitasVaat: '50000.00',
  despesasMde: '50000.00',
  despesasPessoalFundeb: '210000.00',
  despesasCapitalVaat: '8000.00',
  pctMde: '27.78',
  pctFundebPessoal: '75.00',
  pctVaatCapital: '16.00',
  cumpreMde: true,
  cumpreFundebPessoal: true,
  cumpreVaatCapital: true,
};

const calculosFixture = [
  { id: 1, escolaId: 1, etapaId: 3, ano: 2025,
    valorCaqiAlunoAno: '4440.17', valorCaqAlunoAno: '6876.83', gapAdequado: '2436.66' },
  { id: 2, escolaId: 2, etapaId: 3, ano: 2025,
    valorCaqiAlunoAno: '4205.00', valorCaqAlunoAno: '6500.00', gapAdequado: '2295.00' },
];

const calculoDetalheFixture = {
  ...calculosFixture[0],
  itens: [
    { id: 10, perfil: 'minimo', insumoCodigo: 'PES-001', insumoNome: 'Professor — anual',
      tipoAplicacao: 'por_turma', qtdAplicada: '1', custoUnitario: '85000.00',
      custoAnual: '85000.00', divisor: '25', custoAlunoAno: '3400.00',
      baseCalculo: 'PES-001 / 25 alunos por turma' },
    { id: 11, perfil: 'minimo', insumoCodigo: 'MOB-001', insumoNome: 'Carteira escolar',
      tipoAplicacao: 'por_aluno', qtdAplicada: '1', custoUnitario: '280.00',
      custoAnual: '280.00', divisor: '1', custoAlunoAno: '280.00',
      baseCalculo: 'por aluno' },
  ],
};

const contratosFixture = [
  { id: 1, fornecedorId: 1, objeto: 'Limpeza escolar 2025', dataAssinatura: '2025-01-15',
    valorGlobal: '120000.00', modalidade: 'PREGAO_ELETRONICO', pncpId: 'PNCP-2025-001' },
];

const despesasFixture = [
  { id: 1, competencia: '202506', natureza: '3.1.90.11', valor: '50000.00',
    fonteRecursoId: 1, pcasp: '3.1.90.11.00', siopeGrupo: 'MDE',
    contratoId: null, isPessoal: true, isCapital: false },
];

const notificacoesFixture = [
  { id: 1, tipo: 'VIOLACAO_VAAT_15', severidade: 'alta',
    titulo: 'VAAT 15% capital — atenção', descricao: 'Percentual em 16%, próximo do mínimo.',
    anoReferencia: new Date().getFullYear(), baseLegal: 'Lei 14.113/2020',
    payload: null, status: 'aberta', createdAt: '2025-06-15T10:00:00',
    resolvedAt: null },
];

const presetsFixture = [
  { nome: 'tempo_integral_universal', titulo: 'Tempo integral universal',
    descricao: 'Estende a jornada escolar para tempo integral.',
    impactoEsperado: 'CAQ por aluno sobe entre 25% e 40%.',
    baseLegal: ['CF/88 art. 206 IX', 'PNE meta 6'],
    alunosPorTurma: {}, qtdPadraoInsumos: {},
    custoMultiplierInsumos: { 'PES-001': '1.50' },
    etapasRecomendadas: ['CRECHE', 'PRE', 'EF1', 'EF2', 'EM'] },
];

const fontesFixture = [
  { id: 1, tipo: 'Propria', descricao: 'Recursos próprios' },
  { id: 2, tipo: 'VAAF', descricao: 'Fundeb VAAF' },
  { id: 3, tipo: 'VAAT', descricao: 'Fundeb VAAT' },
];

const fornecedoresFixture = [
  { id: 1, cnpj: '00.000.000/0001-91', nome: 'Limpeza Brasil Ltda.',
    optanteSimples: false, municipio: 'Palmas-TO' },
];

const receitasFixture = [
  { id: 1, competencia: '202501', valor: '50000.00', origem: 'impostos', pcasp: null },
];

const insumosFixture = [
  { id: 1, codigo: 'PES-001', nome: 'Professor — anual', categoria: 'Pessoal',
    tipoAplicacao: 'por_turma', unidade: 'docente', etapaAplicavel: 'Todas', qtdPadrao: '1' },
];

const mfaStatus = { enabled: false, username: 'admin_test', backupCodesRemaining: 0 };

const publicacoesFixture = [
  { id: 1, tipo: 'fundeb_execucao', referencia: '2025',
    conteudoHash: '0000aaaa1111bbbb2222cccc3333dddd4444eeee5555ffff6666aaaa7777bbbb',
    tamanhoBytes: 1024,
    urlPublica: 'http://localhost:3000/transparencia/fundeb?ano=2025',
    dataPublicacao: '2025-06-01T12:00:00', snapshot: null },
];

// ── Routes ──────────────────────────────────────────────────────────
const engineRoutes = [
  { method: 'GET',  pattern: /^\/api\/v1\/caqi\/insumos$/,            handler: () => insumosFixture },
  { method: 'GET',  pattern: /^\/api\/v1\/auth\/mfa\/status$/,        handler: () => mfaStatus },
  { method: 'GET',  pattern: /^\/api\/v1\/caqi\/calculos$/,           handler: () => calculosFixture },
  { method: 'GET',  pattern: /^\/api\/v1\/caqi\/calculos\/\d+$/,      handler: () => calculoDetalheFixture },
  { method: 'GET',  pattern: /^\/api\/v1\/caqi\/simulacoes\/presets$/, handler: () => presetsFixture },
  { method: 'POST', pattern: /^\/api\/v1\/caqi\/simulacoes\/presets\/[^/]+$/, handler: () => ({
      ano: 2025, atual: { itens: [] }, simulado: { itens: [] },
      diferencas: [
        { escolaId: '1', etapaCodigo: 'EF1',
          caqiAtual: '4440.17', caqiSimulado: '5290.17', deltaCaqi: '850.00',
          caqAtual: '6876.83', caqSimulado: '8176.83', deltaCaq: '1300.00',
          pctDeltaCaqi: '19.14' },
      ],
    }) },
  { method: 'GET',  pattern: /^\/api\/public\/transparencia\/calculos$/,        handler: () => calculosFixture },
  { method: 'GET',  pattern: /^\/api\/public\/transparencia\/calculos\/\d+$/,   handler: () => calculoDetalheFixture },
  { method: 'GET',  pattern: /^\/api\/public\/transparencia\/insumos$/,         handler: () => insumosFixture },
];

const financeiroRoutes = [
  { method: 'GET',  pattern: /^\/api\/v1\/fundeb\/execucao$/,             handler: () => fundebFixture },
  { method: 'GET',  pattern: /^\/api\/v1\/financeiro\/contratos$/,        handler: () => contratosFixture },
  { method: 'GET',  pattern: /^\/api\/v1\/financeiro\/despesas$/,         handler: () => despesasFixture },
  { method: 'POST', pattern: /^\/api\/v1\/financeiro\/despesas$/,         handler: () => despesasFixture[0] },
  { method: 'GET',  pattern: /^\/api\/v1\/financeiro\/fornecedores$/,     handler: () => fornecedoresFixture },
  { method: 'GET',  pattern: /^\/api\/v1\/financeiro\/fontes-recurso$/,   handler: () => fontesFixture },
  { method: 'GET',  pattern: /^\/api\/v1\/financeiro\/receitas$/,         handler: () => receitasFixture },
  { method: 'GET',  pattern: /^\/api\/public\/transparencia\/fundeb-execucao$/, handler: () => fundebFixture },
  { method: 'GET',  pattern: /^\/api\/public\/transparencia\/contratos$/,       handler: () => contratosFixture },
  { method: 'GET',  pattern: /^\/api\/public\/transparencia\/despesas$/,        handler: () => despesasFixture },
  { method: 'GET',  pattern: /^\/api\/public\/transparencia\/siope-quadro$/,    handler: () => ({
      ano: 2025, tenantMunicipioId: '1721000', geradoEm: '2025-06-01T00:00:00',
      receitas: {}, despesas: {}, vinculacoes: {}, pendencias: [],
    }) },
];

const complianceRoutes = [
  { method: 'GET', pattern: /^\/api\/v1\/compliance\/notificacoes$/,            handler: () => notificacoesFixture },
  { method: 'GET', pattern: /^\/api\/public\/transparencia\/notificacoes$/,     handler: () => notificacoesFixture },
  { method: 'GET', pattern: /^\/api\/public\/transparencia\/publicacoes$/,      handler: () => publicacoesFixture },
];

// Usuários aceitos pelo mock (qualquer senha) — espelham o RBAC do app.
// Em produção tudo é validado contra Basic Auth real do Spring Security.
const VALID_USERS = new Set(['admin_test', 'gestor_test', 'leitor_test']);

function checkBasicAuth(req) {
  const auth = req.headers['authorization'];
  if (!auth || typeof auth !== 'string' || !auth.startsWith('Basic ')) return false;
  try {
    const decoded = Buffer.from(auth.slice(6), 'base64').toString('utf8');
    const [user] = decoded.split(':');
    return VALID_USERS.has(user);
  } catch { return false; }
}

// ── Server factory ──────────────────────────────────────────────────
function makeServer(name, port, routes) {
  const server = http.createServer(async (req, res) => {
    const url = new URL(req.url ?? '/', `http://localhost:${port}`);

    if (url.pathname === '/api/v1/health' || url.pathname.startsWith('/actuator/health')) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'UP' }));
      return;
    }

    if (req.method === 'OPTIONS') {
      res.writeHead(204, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Authorization, Content-Type',
      });
      res.end();
      return;
    }

    // Rotas /api/v1/** exigem Basic Auth. Rotas /api/public/** + /actuator não.
    if (url.pathname.startsWith('/api/v1/') && !checkBasicAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json',
                           'WWW-Authenticate': 'Basic realm="caqi"' });
      res.end(JSON.stringify({ status: 401, message: 'Bad credentials' }));
      return;
    }

    let body = null;
    if (req.method === 'POST') {
      const chunks = [];
      for await (const c of req) chunks.push(c);
      const raw = Buffer.concat(chunks).toString('utf8');
      try { body = raw ? JSON.parse(raw) : null; } catch { body = raw; }
    }

    const route = routes.find(
      (r) => r.method === (req.method ?? 'GET') && r.pattern.test(url.pathname),
    );
    if (!route) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 404, message: `[${name}] no mock for ${req.method} ${url.pathname}` }));
      return;
    }

    try {
      const result = route.handler(url, body);
      res.writeHead(200, {
        'Content-Type': 'application/json',
        'Cache-Control': 'max-age=300, public',
      });
      res.end(JSON.stringify(result));
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 500, message: String(e) }));
    }
  });

  server.listen(port, '127.0.0.1', () => {
    process.stdout.write(`[mock-backend] ${name} listening on 127.0.0.1:${port}\n`);
  });
  return server;
}

// ── Boot ────────────────────────────────────────────────────────────
const servers = [
  makeServer('engine',     9991, engineRoutes),
  makeServer('financeiro', 9992, financeiroRoutes),
  makeServer('escolar',    9993, []),
  makeServer('compliance', 9994, complianceRoutes),
];

function shutdown() {
  process.stdout.write('[mock-backend] shutting down…\n');
  Promise.all(servers.map((s) => new Promise((r) => s.close(() => r()))))
    .then(() => process.exit(0));
}
process.on('SIGTERM', shutdown);
process.on('SIGINT',  shutdown);
