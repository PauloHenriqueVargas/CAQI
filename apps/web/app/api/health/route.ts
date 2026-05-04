import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

export async function GET() {
  return NextResponse.json({
    status: "UP",
    service: "caqi-web",
    timestamp: new Date().toISOString(),
    municipio: {
      id: process.env.CAQI_MUNICIPIO_ID ?? "000000",
      nome: process.env.CAQI_MUNICIPIO_NOME ?? "Município Exemplo",
    },
  });
}
