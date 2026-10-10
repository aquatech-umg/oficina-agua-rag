import type { Metadata } from "next";
import ConsultaForm from "./ConsultaForm";
import styles from "./consulta.module.css";

export const metadata: Metadata = {
  title: "Consulta pública | Oficina Municipal de Agua",
  description: "Consulte el estado de cuenta de su contador de agua.",
};

export default function ConsultaPage() {
  return (
    <main className={styles.main}>
      <h1 className={styles.titulo}>Consulta de estado de cuenta</h1>
      <p className={styles.descripcion}>
        Escriba el código de su contador para ver sus recibos pendientes.
      </p>
      <ConsultaForm />
    </main>
  );
}