package com.appturnos.turnos.plataforma.tenant;

import java.util.Map;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/** Le dice a Hibernate qué negocio filtrar en las entidades con @TenantId. */
@Component
public class TenantResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    /**
     * Hibernate no abre sesiones sin tenant, ni para tablas de plataforma. Sin negocio en el contexto
     * se usa este id inexistente: las consultas de negocio vuelven vacías y los inserts fallan por FK.
     */
    static final Long SIN_NEGOCIO = -1L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long actual = TenantContext.actual();
        return actual != null ? actual : SIN_NEGOCIO;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> props) {
        props.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
