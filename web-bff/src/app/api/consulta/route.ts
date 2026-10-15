import { NextResponse } from "next/server";

interface ErrorResponse {
  codigo: string;
  mensaje: string;
}

const CONSULTA_SERVICE_URL =
  process.env.CONSULTA_SERVICE_URL ?? "http://localhost:8082";

const FORMATO_CODIGO = /^[A-Za-z0-9-]{1,50}$/;
const TIMEOUT_MS = 5000;
const ESTADOS_REENVIADOS = [200, 400, 404, 429];

function error(codigo: string, mensaje: string, status: number) {
  const cuerpo: ErrorResponse = { codigo, mensaje };
  return NextResponse.json(cuerpo, { status });
}

// consulta-service limita por IP (AQ-108) y solo ve la IP de este BFF,
// asi que le pasamos la IP real del visitante en X-Forwarded-For.
function ipDelCliente(request: Request): string {
  const reenviada = request.headers.get("x-forwarded-for");
  const primera = reenviada?.split(",")[0]?.trim();
  return primera || "127.0.0.1";
}

export async function GET(request: Request) {
  const codigo =
    new URL(request.url).searchParams.get("codigo")?.trim().toUpperCase() ?? "";

  if (!FORMATO_CODIGO.test(codigo)) {
    return error(
      "CODIGO_INVALIDO",
      "El código del contador tiene un formato inválido",
      400,
    );
  }

  let respuesta: Response;
  try {
    respuesta = await fetch(
      `${CONSULTA_SERVICE_URL}/api/v1/contadores/${encodeURIComponent(codigo)}/estado-cuenta`,
      {
        headers: { "X-Forwarded-For": ipDelCliente(request) },
        cache: "no-store",
        signal: AbortSignal.timeout(TIMEOUT_MS),
      },
    );
  } catch {
    return error(
      "SERVICIO_NO_DISPONIBLE",
      "El servicio de consulta no está disponible. Intente más tarde",
      502,
    );
  }

  if (!ESTADOS_REENVIADOS.includes(respuesta.status)) {
    return error(
      "SERVICIO_NO_DISPONIBLE",
      "El servicio de consulta no está disponible. Intente más tarde",
      502,
    );
  }

  const headers = new Headers({ "Content-Type": "application/json" });
  const reintentarEn = respuesta.headers.get("Retry-After");
  if (reintentarEn) {
    headers.set("Retry-After", reintentarEn);
  }
  return new NextResponse(await respuesta.text(), {
    status: respuesta.status,
    headers,
  });
}