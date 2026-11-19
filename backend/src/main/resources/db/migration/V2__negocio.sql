-- Tabla de plataforma: no lleva negocio_id, cada fila ES un tenant.
CREATE TABLE negocio (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug         varchar(60)  NOT NULL UNIQUE,
    nombre       varchar(120) NOT NULL,
    zona_horaria varchar(60)  NOT NULL DEFAULT 'America/Argentina/Buenos_Aires',
    creado_en    timestamptz  NOT NULL DEFAULT now()
);
