export default function HomePage() {
  const services = [
    {
      name: "CAQ Engine",
      url: "http://localhost:8081/swagger-ui.html",
      desc: "Motor CAQ/CAQi — parâmetros, insumos, cálculo, simulações.",
    },
    {
      name: "Financeiro",
      url: "http://localhost:8082/swagger-ui.html",
      desc: "Orçamento, Fundeb (70%/15%), MDE 25%, contabilidade, tributário.",
    },
    {
      name: "Escolar",
      url: "http://localhost:8083/swagger-ui.html",
      desc: "Escolas, turmas, matrículas, censo, PNAE/PNATE, pessoal.",
    },
    {
      name: "Compliance",
      url: "http://localhost:8084/swagger-ui.html",
      desc: "Validações legais, auditoria imutável, ROPA/LGPD, transparência LAI, SIOPE.",
    },
  ];

  const municipio = process.env.CAQI_MUNICIPIO_NOME ?? "Município Exemplo";

  return (
    <main className="container mx-auto px-6 py-12">
      <header className="mb-10">
        <p className="text-sm font-medium uppercase tracking-wider text-brand">
          Sistema de Gestão CAQ / CAQi
        </p>
        <h1 className="mt-2 text-4xl font-bold">{municipio}</h1>
        <p className="mt-3 max-w-2xl text-slate-600">
          Painel de desenvolvimento. Cada microsserviço expõe seu próprio
          Swagger UI. A interface produtiva (dashboards executivos, telas
          operacionais) entra na Fase 8 do roadmap.
        </p>
      </header>

      <section>
        <h2 className="mb-4 text-xl font-semibold">Microserviços</h2>
        <div className="grid gap-4 md:grid-cols-2">
          {services.map((s) => (
            <a
              key={s.name}
              href={s.url}
              target="_blank"
              rel="noopener noreferrer"
              className="rounded-lg border border-slate-200 bg-white p-5 transition hover:border-brand hover:shadow-md focus:outline-none focus:ring-2 focus:ring-brand"
            >
              <h3 className="text-lg font-semibold text-brand">{s.name}</h3>
              <p className="mt-1 text-sm text-slate-600">{s.desc}</p>
              <span className="mt-2 inline-block text-xs text-slate-400">
                {s.url}
              </span>
            </a>
          ))}
        </div>
      </section>

      <footer className="mt-16 text-xs text-slate-400">
        Conformidade: CF/88 · LDB · PNE · Lei 14.113/2020 · LRF · LAI · SIAFIC
        · Lei 14.133/2021 · LGPD
      </footer>
    </main>
  );
}
