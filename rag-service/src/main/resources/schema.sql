-- Tabla de fragmentos del RAG (AQ-112). Spring Boot ejecuta este archivo al arrancar.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS fragmentos (
                                          id         BIGSERIAL    PRIMARY KEY,
                                          origen     VARCHAR(30)  NOT NULL,
    archivo    VARCHAR(150) NOT NULL,
    seccion    VARCHAR(200) NOT NULL,
    contenido  TEXT         NOT NULL,
    hash       CHAR(64)     NOT NULL UNIQUE,
    embedding  vector(1024) NOT NULL,
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

-- Indice para buscar por similitud de coseno sin recorrer toda la tabla
CREATE INDEX IF NOT EXISTS fragmentos_embedding_hnsw
    ON fragmentos USING hnsw (embedding vector_cosine_ops);