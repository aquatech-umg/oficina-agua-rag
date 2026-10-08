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

function esUuid(valor: string): boolean {
  const uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

  return uuidRegex.test(valor);
}

export async function POST(request: Request) {
  let body: unknown;

  try {
    body = await request.json();
  } catch {
    const error: ErrorResponse = {
      codigo: "PETICION_INVALIDA",
      mensaje: "El cuerpo de la petición debe ser JSON válido.",
    };

    return NextResponse.json(error, { status: 400 });
  }

  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    const error: ErrorResponse = {
      codigo: "PETICION_INVALIDA",
      mensaje: "El cuerpo de la petición debe ser un objeto JSON válido.",
    };

    return NextResponse.json(error, { status: 400 });
  }

  const datos = body as Record<string, unknown>;
  const mensaje = datos.mensaje;
  const conversacionId = datos.conversacionId;

  if (typeof mensaje !== "string" || mensaje.trim().length === 0) {
    const error: ErrorResponse = {
      codigo: "MENSAJE_INVALIDO",
      mensaje: "El mensaje no puede estar vacío.",
    };

    return NextResponse.json(error, { status: 400 });
  }

  if (mensaje.trim().length > 1000) {
    const error: ErrorResponse = {
      codigo: "MENSAJE_INVALIDO",
      mensaje: "El mensaje no puede superar los 1000 caracteres.",
    };

    return NextResponse.json(error, { status: 400 });
  }

  if (
    conversacionId !== undefined &&
    (typeof conversacionId !== "string" || !esUuid(conversacionId))
  ) {
    const error: ErrorResponse = {
      codigo: "CONVERSACION_INVALIDA",
      mensaje: "El conversacionId debe tener un formato UUID válido.",
    };

    return NextResponse.json(error, { status: 400 });
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

  return NextResponse.json(respuesta, { status: 200 });
}