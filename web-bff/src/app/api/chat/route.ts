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

interface ChatRequestPayload {
  mensaje: string;
  conversacionId?: string;
}

const ORIGENES_PERMITIDOS = (process.env.WIDGET_ALLOWED_ORIGINS ?? "")
  .split(",")
  .map((origen) => origen.trim())
  .filter(Boolean);

const RAG_SERVICE_URL = process.env.RAG_SERVICE_URL?.replace(/\/$/, "");

function obtenerTimeoutRag(): number {
  const valor = Number.parseInt(
    process.env.RAG_SERVICE_TIMEOUT_MS ?? "60000",
    10
  );

  return Number.isFinite(valor) && valor > 0 ? valor : 60000;
}

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

function esErrorResponse(valor: unknown): valor is ErrorResponse {
  if (
    typeof valor !== "object" ||
    valor === null ||
    Array.isArray(valor)
  ) {
    return false;
  }

  const datos = valor as Record<string, unknown>;

  return (
    typeof datos.codigo === "string" &&
    typeof datos.mensaje === "string"
  );
}

function esChatResponse(valor: unknown): valor is ChatResponse {
  if (
    typeof valor !== "object" ||
    valor === null ||
    Array.isArray(valor)
  ) {
    return false;
  }

  const datos = valor as Record<string, unknown>;

  return (
    typeof datos.respuesta === "string" &&
    typeof datos.conversacionId === "string"
  );
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

  if (
    typeof body !== "object" ||
    body === null ||
    Array.isArray(body)
  ) {
    const error: ErrorResponse = {
      codigo: "PETICION_INVALIDA",
      mensaje: "El cuerpo de la petición debe ser un objeto JSON válido.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  const datos = body as Record<string, unknown>;
  const mensaje = datos.mensaje;
  const conversacionId = datos.conversacionId;

  if (
    typeof mensaje !== "string" ||
    mensaje.trim().length === 0
  ) {
    const error: ErrorResponse = {
      codigo: "MENSAJE_INVALIDO",
      mensaje: "El mensaje no puede estar vacío.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  const mensajeLimpio = mensaje.trim();

  if (mensajeLimpio.length > 1000) {
    const error: ErrorResponse = {
      codigo: "MENSAJE_INVALIDO",
      mensaje: "El mensaje no puede superar los 1000 caracteres.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  if (
    conversacionId !== undefined &&
    (
      typeof conversacionId !== "string" ||
      !esUuid(conversacionId)
    )
  ) {
    const error: ErrorResponse = {
      codigo: "CONVERSACION_INVALIDA",
      mensaje:
        "El conversacionId debe tener un formato UUID válido.",
    };

    return respuestaJson(error, 400, origen, request.url);
  }

  if (!RAG_SERVICE_URL) {
    const error: ErrorResponse = {
      codigo: "SERVICIO_NO_DISPONIBLE",
      mensaje:
        "El servicio del asistente no está configurado en este momento.",
    };

    return respuestaJson(error, 503, origen, request.url);
  }

  const payload: ChatRequestPayload = {
    mensaje: mensajeLimpio,
  };

  if (typeof conversacionId === "string") {
    payload.conversacionId = conversacionId;
  }

  const controlador = new AbortController();
  const timeout = setTimeout(
    () => controlador.abort(),
    obtenerTimeoutRag()
  );

  try {
    const respuestaRag = await fetch(
      `${RAG_SERVICE_URL}/api/chat`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
        signal: controlador.signal,
        cache: "no-store",
      }
    );

    let datosRag: unknown;

    try {
      datosRag = await respuestaRag.json();
    } catch {
      const error: ErrorResponse = {
        codigo: "RESPUESTA_INVALIDA",
        mensaje:
          "El servicio del asistente devolvió una respuesta no válida.",
      };

      return respuestaJson(error, 502, origen, request.url);
    }

    if (!respuestaRag.ok) {
      if (esErrorResponse(datosRag)) {
        return respuestaJson(
          datosRag,
          respuestaRag.status,
          origen,
          request.url
        );
      }

      const error: ErrorResponse = {
        codigo: "SERVICIO_NO_DISPONIBLE",
        mensaje:
          "El asistente no está disponible en este momento. Intenta nuevamente.",
      };

      return respuestaJson(
        error,
        respuestaRag.status >= 500 ? 503 : respuestaRag.status,
        origen,
        request.url
      );
    }

    if (!esChatResponse(datosRag)) {
      const error: ErrorResponse = {
        codigo: "RESPUESTA_INVALIDA",
        mensaje:
          "El servicio del asistente devolvió una respuesta no válida.",
      };

      return respuestaJson(error, 502, origen, request.url);
    }

    return respuestaJson(
      datosRag,
      200,
      origen,
      request.url
    );
  } catch (error) {
    if (
      error instanceof Error &&
      error.name === "AbortError"
    ) {
      const respuestaTimeout: ErrorResponse = {
        codigo: "TIEMPO_AGOTADO",
        mensaje:
          "El asistente está tardando más de lo esperado. Intenta nuevamente en unos momentos.",
      };

      return respuestaJson(
        respuestaTimeout,
        504,
        origen,
        request.url
      );
    }

    console.error(
      "Error al comunicarse con rag-service:",
      error
    );

    const respuestaError: ErrorResponse = {
      codigo: "SERVICIO_NO_DISPONIBLE",
      mensaje:
        "No fue posible comunicarse con el asistente. Intenta nuevamente.",
    };

    return respuestaJson(
      respuestaError,
      503,
      origen,
      request.url
    );
  } finally {
    clearTimeout(timeout);
  }
}
