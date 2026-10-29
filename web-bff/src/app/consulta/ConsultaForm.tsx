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
  mora: number;
  total: number;
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
        {
          headers: {
            "X-Captcha-Token": token,
          },
        },
      );

      if (respuesta.ok) {
        const datos = (await respuesta.json()) as EstadoCuenta;

        setCodigo(normalizado);
        setResultado(datos);

        return;
      }

      if (respuesta.status === 404) {
        setError("No existe un contador con ese código.");
      } else if (respuesta.status === 403) {
        setError(
          "No se pudo verificar el captcha. Resuélvalo de nuevo e intente otra vez.",
        );
      } else if (respuesta.status === 429) {
        setError(
          "Demasiadas consultas seguidas. Intente de nuevo en un momento.",
        );
      } else {
        setError(
          "No se pudo consultar en este momento. Intente más tarde.",
        );
      }
    } catch {
      setError(
        "No se pudo consultar en este momento. Intente más tarde.",
      );
    } finally {
      setCargando(false);
      reiniciarCaptcha();
    }
  }

  if (!sitekey) {
    return (
      <div className={styles.error} role="alert">
        <span aria-hidden="true">!</span>
        <p>
          La consulta no está disponible porque falta configurar el captcha.
        </p>
      </div>
    );
  }

  return (
    <>
      <form
        className={styles.formulario}
        onSubmit={manejarEnvio}
        noValidate
      >
        <div className={styles.formularioEncabezado}>
          <h2>Ingrese sus Datos</h2>

          <p>
            El código identifica su servicio y permite consultar únicamente la información pública de su estado de cuenta.
          </p>
        </div>

        <div className={styles.grupoCampo}>
          <label
            className={styles.etiqueta}
            htmlFor="codigo"
          >
            Código del contador
          </label>

          <input
            id="codigo"
            className={styles.campo}
            type="text"
            value={codigo}
            onChange={(evento) =>
              setCodigo(evento.target.value.toUpperCase())
            }
            placeholder="CONT-001"
            autoComplete="off"
            spellCheck={false}
            maxLength={11}
            aria-describedby="ayuda-codigo"
          />

          <p
            id="ayuda-codigo"
            className={styles.ayuda}
          >
            Ejemplo: <strong>CONT-001</strong>
          </p>
        </div>

        <div className={styles.captcha}>
          <HCaptcha
            ref={captchaRef}
            sitekey={sitekey}
            languageOverride="es"
            onVerify={(nuevoToken) => {
              setToken(nuevoToken);
              setError(null);
            }}
            onExpire={() => {
              setToken(null);
            }}
            onError={() => {
              setToken(null);
              setError(
                "No se pudo cargar el captcha. Recargue la página.",
              );
            }}
          />
        </div>

        <button
          className={styles.boton}
          type="submit"
          disabled={cargando}
        >
          {cargando && (
            <span
              className={styles.spinner}
              aria-hidden="true"
            />
          )}

          {cargando ? "Consultando..." : "Consultar cuenta"}
        </button>

        {error && (
          <div
            className={styles.error}
            role="alert"
          >
            <span aria-hidden="true">!</span>
            <p>{error}</p>
          </div>
        )}
      </form>

      {resultado && (
        <section
          className={styles.resultado}
          aria-live="polite"
        >
          <div className={styles.resultadoEncabezado}>
            <div>
              <span className={styles.etiquetaResumen}>
                Estado de cuenta
              </span>

              <h2>{resultado.codigoContador}</h2>
            </div>

            <div className={styles.saldo}>
              <span>Saldo pendiente</span>

              <strong>
                {quetzales(resultado.saldoPendiente)}
              </strong>
            </div>
          </div>

          <dl className={styles.datosServicio}>
            <div>
              <dt>Dirección del servicio</dt>
              <dd>{resultado.direccionServicio}</dd>
            </div>

            <div>
              <dt>Sector</dt>
              <dd>{resultado.sector ?? "No especificado"}</dd>
            </div>
          </dl>

          {resultado.recibosPendientes.length === 0 ? (
            <div
              className={styles.alDia}
              role="status"
            >
              <span
                className={styles.alDiaIcono}
                aria-hidden="true"
              >
                ✓
              </span>

              <div>
                <strong>Está al día</strong>
                <p>No tiene recibos pendientes.</p>
              </div>
            </div>
          ) : (
            <div className={styles.recibos}>
              <div className={styles.recibosEncabezado}>
                <div>
                  <h3>Recibos pendientes</h3>

                  <p>
                    {resultado.recibosPendientes.length}{" "}
                    {resultado.recibosPendientes.length === 1
                      ? "recibo por pagar"
                      : "recibos por pagar"}
                  </p>
                </div>

                <span>
                  Deslice horizontalmente en celular
                </span>
              </div>

              <div className={styles.tablaContenedor}>
                <table className={styles.tabla}>
                  <thead>
                    <tr>
                      <th scope="col">Recibo</th>
                      <th scope="col">Periodo</th>
                      <th scope="col">Emisión</th>

                      <th
                        scope="col"
                        className={styles.derecha}
                      >
                        Monto
                      </th>

                      <th
                        scope="col"
                        className={styles.derecha}
                      >
                        Mora
                      </th>

                      <th
                        scope="col"
                        className={styles.derecha}
                      >
                        Total a pagar
                      </th>
                    </tr>
                  </thead>

                  <tbody>
                    {resultado.recibosPendientes.map(
                      (recibo) => (
                        <tr key={recibo.numeroRecibo}>
                          <td>{recibo.numeroRecibo}</td>
                          <td>{recibo.periodo}</td>
                          <td>{recibo.fechaEmision}</td>

                          <td className={styles.derecha}>
                            {quetzales(recibo.monto)}
                          </td>

                          <td className={styles.derecha}>
                            {quetzales(recibo.mora)}
                          </td>

                          <td
                            className={`${styles.derecha} ${styles.total}`}
                          >
                            {quetzales(recibo.total)}
                          </td>
                        </tr>
                      ),
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>
      )}
    </>
  );
}