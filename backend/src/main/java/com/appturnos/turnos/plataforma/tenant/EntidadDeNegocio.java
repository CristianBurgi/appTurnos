package com.appturnos.turnos.plataforma.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.TenantId;

/**
 * Base de toda entidad que pertenece a un negocio. Hibernate completa negocio_id al
 * insertar y agrega "where negocio_id = ?" en cada consulta JPA/JPQL (no en SQL nativo).
 */
@MappedSuperclass
public abstract class EntidadDeNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "negocio_id", nullable = false, updatable = false)
    private Long negocioId;

    public Long getId() { return id; }
    public Long getNegocioId() { return negocioId; }
}
