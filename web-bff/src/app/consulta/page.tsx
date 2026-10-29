import type { Metadata } from "next";
import Image from "next/image";
import Script from "next/script";

import ConsultaForm from "./ConsultaForm";
import styles from "./consulta.module.css";

export const metadata: Metadata = {
  title: "Consulta pública | Oficina Municipal de Agua",
  description: "Consulte su estado de cuenta de forma rápida y segura.",
};

export default function ConsultaPage() {
  // La sitekey es publica; se lee en el servidor y se pasa al formulario.
  const sitekey = process.env.HCAPTCHA_SITEKEY ?? "";

  return (
    <div className={styles.pagina}>
      <header className={styles.encabezado}>
        <div className={styles.encabezadoContenido}>
          <div className={styles.identidad}>
            <span className={styles.identidadPrincipal}>
              Consulta
            </span>

            <span className={styles.identidadDestacada}>
              en línea
            </span>
          </div>

          <div className={styles.logoAquatech}>
            <Image
              src="/img/Logo_AquatechGt.png"
              alt="AquaTech GT"
              width={240}
              height={90}
              priority
              className={styles.logoAquatechImagen}
            />
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <section className={styles.presentacion}>
          <h1 className={styles.titulo}>
            <span>Consulte</span>

            <span className={styles.tituloDestacado}>
              Estado de Cuenta
            </span>
          </h1>

          <p className={styles.descripcion}>
            Consulte sus recibos pendientes y saldo actual.
          </p>
        </section>

        <ConsultaForm sitekey={sitekey} />
      </main>

      <footer className={styles.pie}>
        <div className={styles.pieContenido}>
          <div>
            <p className={styles.pieTitulo}>
              Oficina Municipal de Agua
            </p>

            <p className={styles.pieTexto}>
              Servicios digitales para nuestros usuarios.
            </p>
          </div>

          <div className={styles.pieDato}>
            <span>Atención</span>
            <strong>
              Consulte al asistente virtual
            </strong>
          </div>

          <div className={styles.pieDato}>
            <span>Información</span>
            <strong>
              Horarios y servicios disponibles
            </strong>
          </div>
        </div>
      </footer>

      <Script
        src="/chat-widget.js"
        strategy="afterInteractive"
      />
    </div>
  );
}