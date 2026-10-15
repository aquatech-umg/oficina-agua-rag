"use client";

import HCaptcha from "@hcaptcha/react-hcaptcha";
import { useRef, useState, type FormEvent } from "react";
import styles from "./consulta.module.css";

const FORMATO_CODIGO = /^CONT-\d{3,6}$/;

interface ReciboPendiente {
  numeroRecibo: string;
  periodo: string;
  fechaEmision: string;
  monto: number;
}

interface EstadoCuenta {
  codigoContador: string;
  direccionServicio: string;
  sector: string | null;
  saldoPendiente: number;
  recibosPendientes: ReciboPendiente[];
}

interface ConsultaFormProps {
  sitekey: string;
}

function quetzales(valor: number): string {
  return `Q${valor.toFixed(2)}`;
}

export default function ConsultaForm({ sitekey }: ConsultaFormProps) {
  const captchaRef = useRef<HCaptcha>(null);
  const [codigo, setCodigo] = useState("");
  const [token, setToken] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resultado, setResultado] = useState<EstadoCuenta | null>(null);

  // Cada token de hCaptcha sirve una sola vez: despues de cada consulta
  // hay que pedir un captcha nuevo.
  function reiniciarCaptcha() {
    captchaRef.current?.resetCaptcha();
    setToken(null);
  }

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const normalizado = codigo.trim().toUpperCase();

    setResultado(null);
    if (!FORMATO_CODIGO.test(normalizado)) {
      setError("Escriba un código válido, por ejemplo CONT-001.");
      return;
    }
    if (!token) {
      setError("Resuelva el captcha antes de consultar.");
      return;
    }

    setError(null);
    setCargando(true);
    try {
      const respuesta = await fetch(
        `/api/consulta?codigo=${encodeURIComponent(normalizado)}`,
        { headers: { "X-Captcha-Token": token } },
      );

      if (respuesta.ok) {
        setResultado((await respuesta.json()) as EstadoCuenta);
        return;
      }

      if (respuesta.status === 404) {
        setError("No existe un contador con ese código.");
      } else if (respuesta.status === 403) {
        setError("No se pudo verificar el captcha. Resuélvalo de nuevo e intente otra vez.");
      } else if (respuesta.status === 429) {
        setError("Demasiadas consultas seguidas. Intente de nuevo en un momento.");
      } else {
        setError("No se pudo consultar en este momento. Intente más tarde.");
      }
    } catch {
      setError("No se pudo consultar en este momento. Intente más tarde.");
    } finally {
      setCargando(false);
      reiniciarCaptcha();
    }
  }

  if (!sitekey) {
    return (
      <p className={styles.error} role="alert">
        La consulta no está disponible: falta configurar el captcha (HCAPTCHA_SITEKEY).
      </p>
    );
  }

  return (
    <>
      <form className={styles.formulario} onSubmit={manejarEnvio} noValidate>
        <label className={styles.etiqueta} htmlFor="codigo">
          Código del contador
        </label>
        <input
          id="codigo"
          className={styles.campo}
          type="text"
          value={codigo}
          onChange={(evento) => setCodigo(evento.target.value)}
          placeholder="CONT-001"
          autoComplete="off"
        />
        <HCaptcha
          ref={captchaRef}
          sitekey={sitekey}
          languageOverride="es"
          onVerify={(nuevoToken) => {
            setToken(nuevoToken);
            setError(null);
          }}
          onExpire={() => setToken(null)}
          onError={() => {
            setToken(null);
            setError("No se pudo cargar el captcha. Recargue la página.");
          }}
        />
        <button className={styles.boton} type="submit" disabled={cargando}>
          {cargando ? "Consultando..." : "Consultar"}
        </button>
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}
      </form>

      {resultado && (
        <section className={styles.resultado}>
          <h2>{resultado.codigoContador}</h2>
          <p>
            {resultado.direccionServicio}
            {resultado.sector ? ` (${resultado.sector})` : ""}
          </p>
          <p className={styles.saldo}>
            Saldo pendiente: {quetzales(resultado.saldoPendiente)}
          </p>
          {resultado.recibosPendientes.length === 0 ? (
            <p className={styles.aviso}>No tiene recibos pendientes.</p>
          ) : (
            <table className={styles.tabla}>
              <thead>
                <tr>
                  <th>Recibo</th>
                  <th>Periodo</th>
                  <th>Emisión</th>
                  <th className={styles.derecha}>Monto</th>
                </tr>
              </thead>
              <tbody>
                {resultado.recibosPendientes.map((recibo) => (
                  <tr key={recibo.numeroRecibo}>
                    <td>{recibo.numeroRecibo}</td>
                    <td>{recibo.periodo}</td>
                    <td>{recibo.fechaEmision}</td>
                    <td className={styles.derecha}>{quetzales(recibo.monto)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </>
  );
}