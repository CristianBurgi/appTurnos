package com.appturnos.turnos.plataforma.tenant;

import jakarta.persistence.Entity;

/** Entidad de negocio mínima, solo para probar el aislamiento. */
@Entity
public class NotaPrueba extends EntidadDeNegocio {

    private String texto;

    protected NotaPrueba() {
    }

    NotaPrueba(String texto) {
        this.texto = texto;
    }

    String getTexto() { return texto; }
}
