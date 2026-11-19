-- Solo tests: tabla mínima con negocio_id para probar el aislamiento antes de tener entidades reales.
CREATE TABLE nota_prueba (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    negocio_id bigint NOT NULL REFERENCES negocio (id),
    texto      varchar(100) NOT NULL
);
