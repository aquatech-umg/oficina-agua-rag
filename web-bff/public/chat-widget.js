(() => {
  "use strict";

  const scriptActual = document.currentScript;

  const origenBff = scriptActual?.dataset.apiBase
    ? scriptActual.dataset.apiBase.replace(/\/$/, "")
    : scriptActual?.src
      ? new URL(scriptActual.src).origin
      : window.location.origin;

  let conversacionId = null;
  let abierto = false;
  let enviando = false;

  const host = document.createElement("div");
  host.id = "aquatech-chat-widget";

  const shadow = host.attachShadow({ mode: "open" });

  const estilos = document.createElement("style");

  estilos.textContent = `
    * {
      box-sizing: border-box;
    }

    :host {
      font-family: Arial, Helvetica, sans-serif;
    }

    .chat-bubble {
      position: fixed;
      right: 24px;
      bottom: 24px;
      width: 60px;
      height: 60px;
      border: none;
      border-radius: 50%;
      background: #0d6efd;
      color: #ffffff;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.22);
      z-index: 2147483647;
      transition:
        transform 0.2s ease,
        box-shadow 0.2s ease;
    }

    .chat-bubble:hover {
      transform: scale(1.06);
      box-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
    }

    .chat-bubble:focus-visible {
      outline: 3px solid rgba(13, 110, 253, 0.3);
      outline-offset: 3px;
    }

    .chat-bubble svg {
      width: 28px;
      height: 28px;
      fill: currentColor;
    }

    .chat-window {
      position: fixed;
      right: 24px;
      bottom: 96px;
      width: 360px;
      max-width: calc(100vw - 32px);
      height: 510px;
      max-height: calc(100vh - 130px);
      background: #ffffff;
      border-radius: 16px;
      box-shadow: 0 16px 45px rgba(0, 0, 0, 0.22);
      overflow: hidden;
      display: none;
      flex-direction: column;
      z-index: 2147483646;
      border: 1px solid #e7e7e7;
    }

    .chat-window.open {
      display: flex;
    }

    .chat-header {
      background: #0d6efd;
      color: #ffffff;
      padding: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .chat-header-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .chat-title {
      margin: 0;
      font-size: 16px;
      font-weight: 700;
    }

    .chat-subtitle {
      margin: 0;
      font-size: 12px;
      opacity: 0.9;
    }

    .chat-close {
      border: none;
      background: transparent;
      color: #ffffff;
      cursor: pointer;
      width: 34px;
      height: 34px;
      border-radius: 50%;
      font-size: 25px;
      line-height: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .chat-close:hover {
      background: rgba(255, 255, 255, 0.14);
    }

    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      background: #f7f8fa;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .message {
      display: flex;
      flex-direction: column;
      max-width: 82%;
    }

    .message.user {
      align-self: flex-end;
      align-items: flex-end;
    }

    .message.bot {
      align-self: flex-start;
      align-items: flex-start;
    }

    .message-content {
      padding: 10px 13px;
      border-radius: 14px;
      font-size: 14px;
      line-height: 1.45;
      word-break: break-word;
      white-space: pre-wrap;
    }

    .message.user .message-content {
      background: #0d6efd;
      color: #ffffff;
      border-bottom-right-radius: 4px;
    }

    .message.bot .message-content {
      background: #ffffff;
      color: #212529;
      border: 1px solid #e1e4e8;
      border-bottom-left-radius: 4px;
    }

    .sources {
      margin-top: 5px;
      font-size: 11px;
      color: #6c757d;
      line-height: 1.35;
    }

    .chat-form {
      padding: 12px;
      background: #ffffff;
      border-top: 1px solid #e7e7e7;
      display: flex;
      gap: 8px;
      align-items: flex-end;
    }

    .chat-input {
      flex: 1;
      min-height: 42px;
      max-height: 90px;
      resize: none;
      border: 1px solid #ced4da;
      border-radius: 10px;
      padding: 10px 12px;
      font-family: inherit;
      font-size: 14px;
      line-height: 1.35;
      outline: none;
    }

    .chat-input:focus {
      border-color: #0d6efd;
      box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.12);
    }

    .chat-send {
      width: 42px;
      height: 42px;
      flex-shrink: 0;
      border: none;
      border-radius: 10px;
      background: #0d6efd;
      color: #ffffff;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .chat-send:hover:not(:disabled) {
      background: #0b5ed7;
    }

    .chat-send:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }

    .chat-send svg {
      width: 20px;
      height: 20px;
      fill: currentColor;
    }

    .typing {
      font-size: 12px;
      color: #6c757d;
      font-style: italic;
      padding: 2px 4px;
    }

    @media (max-width: 480px) {
      .chat-bubble {
        right: 16px;
        bottom: 16px;
      }

      .chat-window {
        right: 16px;
        bottom: 88px;
        width: calc(100vw - 32px);
        height: min(510px, calc(100vh - 112px));
      }
    }
  `;

  const contenedor = document.createElement("div");

  contenedor.innerHTML = `
    <section
      class="chat-window"
      role="dialog"
      aria-label="Chat de la Oficina Municipal de Agua"
      aria-hidden="true"
    >
      <header class="chat-header">
        <div class="chat-header-info">
          <p class="chat-title">Asistente de Agua</p>
          <p class="chat-subtitle">Oficina Municipal de Agua</p>
        </div>

        <button
          type="button"
          class="chat-close"
          aria-label="Cerrar chat"
        >
          &times;
        </button>
      </header>

      <div
        class="chat-messages"
        role="log"
        aria-live="polite"
      ></div>

      <form class="chat-form">
        <textarea
          class="chat-input"
          rows="1"
          maxlength="1000"
          placeholder="Escribe tu consulta..."
          aria-label="Mensaje para el asistente"
        ></textarea>

        <button
          type="submit"
          class="chat-send"
          aria-label="Enviar mensaje"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M2.01 21 23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </form>
    </section>

    <button
      type="button"
      class="chat-bubble"
      aria-label="Abrir chat"
      aria-expanded="false"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M20 2H4a2 2 0 0 0-2 2v18l4-4h14a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2z"/>
      </svg>
    </button>
  `;

  shadow.append(estilos, contenedor);

  const burbuja = shadow.querySelector(".chat-bubble");
  const ventana = shadow.querySelector(".chat-window");
  const cerrar = shadow.querySelector(".chat-close");
  const mensajes = shadow.querySelector(".chat-messages");
  const formulario = shadow.querySelector(".chat-form");
  const entrada = shadow.querySelector(".chat-input");
  const botonEnviar = shadow.querySelector(".chat-send");

  function agregarMensaje(texto, tipo, fuentes = []) {
    const mensaje = document.createElement("div");
    mensaje.className = `message ${tipo}`;

    const contenido = document.createElement("div");
    contenido.className = "message-content";
    contenido.textContent = texto;

    mensaje.appendChild(contenido);

    if (tipo === "bot" && Array.isArray(fuentes) && fuentes.length > 0) {
      const fuentesElemento = document.createElement("div");
      fuentesElemento.className = "sources";

      const titulos = fuentes
        .map((fuente) => fuente?.titulo)
        .filter(Boolean);

      if (titulos.length > 0) {
        fuentesElemento.textContent = `Fuentes: ${titulos.join(", ")}`;
        mensaje.appendChild(fuentesElemento);
      }
    }

    mensajes.appendChild(mensaje);
    mensajes.scrollTop = mensajes.scrollHeight;
  }

  function mostrarEscribiendo() {
    const elemento = document.createElement("div");
    elemento.className = "typing";
    elemento.id = "typing-indicator";
    elemento.textContent = "El asistente está respondiendo...";

    mensajes.appendChild(elemento);
    mensajes.scrollTop = mensajes.scrollHeight;
  }

  function ocultarEscribiendo() {
    const elemento = mensajes.querySelector("#typing-indicator");

    if (elemento) {
      elemento.remove();
    }
  }

  function cambiarEstadoChat() {
    abierto = !abierto;

    ventana.classList.toggle("open", abierto);
    ventana.setAttribute("aria-hidden", String(!abierto));
    burbuja.setAttribute("aria-expanded", String(abierto));
    burbuja.setAttribute(
      "aria-label",
      abierto ? "Cerrar chat" : "Abrir chat"
    );

    if (abierto) {
      entrada.focus();
    }
  }

  async function enviarMensaje(texto) {
    if (enviando) {
      return;
    }

    enviando = true;
    botonEnviar.disabled = true;

    agregarMensaje(texto, "user");
    mostrarEscribiendo();

    const payload = {
      mensaje: texto,
    };

    if (conversacionId) {
      payload.conversacionId = conversacionId;
    }

    try {
      const respuestaHttp = await fetch(`${origenBff}/api/chat`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const datos = await respuestaHttp.json();

      ocultarEscribiendo();

      if (!respuestaHttp.ok) {
        agregarMensaje(
          datos?.mensaje ||
            "No fue posible procesar la solicitud.",
          "bot"
        );

        return;
      }

      conversacionId = datos.conversacionId ?? conversacionId;

      agregarMensaje(
        datos.respuesta || "No se recibió una respuesta.",
        "bot",
        datos.fuentes
      );
    } catch (error) {
      ocultarEscribiendo();

      console.error(
        "Error al comunicarse con el servicio de chat:",
        error
      );

      agregarMensaje(
        "En este momento no es posible comunicarse con el asistente. Intenta nuevamente.",
        "bot"
      );
    } finally {
      enviando = false;
      botonEnviar.disabled = false;
      entrada.focus();
    }
  }

  burbuja.addEventListener("click", cambiarEstadoChat);

  cerrar.addEventListener("click", () => {
    if (abierto) {
      cambiarEstadoChat();
    }
  });

  formulario.addEventListener("submit", async (event) => {
    event.preventDefault();

    const texto = entrada.value.trim();

    if (!texto) {
      return;
    }

    entrada.value = "";

    await enviarMensaje(texto);
  });

  entrada.addEventListener("keydown", (event) => {
    if (
      event.key === "Enter" &&
      !event.shiftKey &&
      !event.isComposing
    ) {
      event.preventDefault();
      formulario.requestSubmit();
    }
  });

  agregarMensaje(
    "Hola. Soy el asistente virtual de la Oficina Municipal de Agua. ¿En qué puedo ayudarte?",
    "bot"
  );

  document.body.appendChild(host);
})();