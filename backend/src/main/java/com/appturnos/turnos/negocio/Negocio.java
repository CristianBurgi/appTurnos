package com.appturnos.turnos.negocio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** El tenant. Tabla de plataforma: sin @TenantId. */
@Entity
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;

    private String nombre;

    private String zonaHoraria = "America/Argentina/Buenos_Aires";

    @Column(insertable = false, updatable = false)
    private Instant creadoEn;

    protected Negocio() {
    }

    public Negocio(String slug, String nombre) {
        this.slug = slug;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getNombre() { return nombre; }
    public String getZonaHoraria() { return zonaHoraria; }
    public Instant getCreadoEn() { return creadoEn; }
}
