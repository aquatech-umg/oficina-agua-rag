"use client";

import { useState, type FormEvent } from "react";
import styles from "./consulta.module.css";

const FORMATO_CODIGO = /^CONT-\d{3,6}$/;

export default function ConsultaForm() {
  const [codigo, setCodigo] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const normalizado = codigo.trim().toUpperCase();

    if (!FORMATO_CODIGO.test(normalizado)) {
      setAviso(null);
      setError("Escriba un código válido, por ejemplo CONT-001.");
      return;
    }

    setError(null);
    setAviso(
      `Código ${normalizado} válido. La consulta al servicio se conectará en la siguiente tarea.`,
    );
  }

  return (
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
      <button className={styles.boton} type="submit">
        Consultar
      </button>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      {aviso && <p className={styles.aviso}>{aviso}</p>}
    </form>
  );
}