import { randomUUID } from "node:crypto";
import { NextResponse } from "next/server";

interface Fuente {
  titulo: string;
  fragmento: string;
}

interface ChatResponse {
  respuesta: string;
  conversacionId: string;
  fuentes?: Fuente[];
}

interface ErrorResponse {
  codigo: string;
  mensaje: string;
}

const ORIGENES_PERMITIDOS = (process.env.WIDGET_ALLOWED_ORIGINS ?? "")
  .split(",")
  .map((origen) => origen.trim())
  .filter(Boolean);

function esOrigenPermitido(
  origen: string | null,
  urlSolicitud: string
): boolean {
  if (!origen) {
    return true;
  }

  const origenBff = new URL(urlSolicitud).origin;

  return (
    origen === origenBff ||
    ORIGENES_PERMITIDOS.includes(origen)
  );
}

function encabezadosCors(
  origen: string | null,
  urlSolicitud: string
): HeadersInit {
  const headers: Record<string, string> = {
    Vary: "Origin",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
  };

  if (origen && esOrigenPermitido(origen, urlSolicitud)) {
    headers["Access-Control-Allow-Origin"] = origen;
  }

  return headers;
}

function respuestaJson<T>(
  datos: T,
  status: number,
  origen: string | null,
  urlSolicitud: string
): NextResponse<T> {
  return NextResponse.json(datos, {
    status,
    headers: encabezadosCors(origen, urlSolicitud),
  });
}

function esUuid(valor: string): boolean {
  const uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  return uuidRegex.test(valor);
}

export async function OPTIONS(request: Request) {
  const origen = request.headers.get("origin");

  if (!esOrigenPermitido(origen, request.url)) {
    const error: ErrorResponse = {
      codigo: "ORIGEN_NO_PERMITIDO",
      mensaje: "El origen de la solicitud no está permitido.",
    };

    return respuestaJson(error, 403, origen, request.url);
  }

  return new NextResponse(null, {
    status: 204,
    headers: {
      ...encabezadosCors(origen, request.url),
      "Access-Control-Max-Age": "86400",
    },
  });
}

export async function POST(request: Request) {
  const origen = request.headers.get("origin");

  if (!esOrigenPermitido(origen, request.url)) {
    const error: ErrorResponse = {
      codigo: "ORIGEN_NO_PERMITIDO",
      mensaje: "El origen de la solicitud no está permitido.",
    };

    return respuestaJson(error, 403, origen, request.url);
  }

  let body: unknown;

  try {
    body = await request.json();
  } catch {
    const error: ErrorResponse = {
      codigo: "PETICION_INVALIDA",
      mensaje: "El cuerpo de la petición debe ser JSON válido.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    const error: ErrorResponse = {
      codigo: "PETICION_INVALIDA",
      mensaje: "El cuerpo de la petición debe ser un objeto JSON válido.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  const datos = body as Record<string, unknown>;
  const mensaje = datos.mensaje;
  const conversacionId = datos.conversacionId;

  if (typeof mensaje !== "string" || mensaje.trim().length === 0) {
    const error: ErrorResponse = {
      codigo: "MENSAJE_INVALIDO",
      mensaje: "El mensaje no puede estar vacío.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  if (mensaje.trim().length > 1000) {
    const error: ErrorResponse = {
      codigo: "MENSAJE_INVALIDO",
      mensaje: "El mensaje no puede superar los 1000 caracteres.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  if (
    conversacionId !== undefined &&
    (typeof conversacionId !== "string" || !esUuid(conversacionId))
  ) {
    const error: ErrorResponse = {
      codigo: "CONVERSACION_INVALIDA",
      mensaje: "El conversacionId debe tener un formato UUID válido.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  const idConversacion =
    typeof conversacionId === "string" ? conversacionId : randomUUID();

  const respuesta: ChatResponse = {
    respuesta:
      "Esta es una respuesta simulada del chatbot de la Oficina Municipal de Agua. La integración con el servicio RAG se realizará posteriormente.",
    conversacionId: idConversacion,
    fuentes: [
      {
        titulo: "Contenido de referencia simulado",
        fragmento:
          "Respuesta temporal utilizada para probar el contrato entre Next.js y el servicio RAG.",
      },
    ],
  };

  return respuestaJson(respuesta, 200, origen, request.url);
}