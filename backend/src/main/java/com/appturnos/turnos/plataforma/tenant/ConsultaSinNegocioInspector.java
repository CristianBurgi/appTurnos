package com.appturnos.turnos.plataforma.tenant;

import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Sin negocio activo, las consultas de negocio vuelven vacías (ver {@link TenantResolver#SIN_NEGOCIO}).
 * Para que ese vacío no esconda un bug, se loguea WARN cada SQL que toca una tabla de negocio sin negocio activo.
 */
@Component
public class ConsultaSinNegocioInspector implements StatementInspector, HibernatePropertiesCustomizer {

    private static final Logger log = LoggerFactory.getLogger(ConsultaSinNegocioInspector.class);

    private final ObjectProvider<EntityManagerFactory> emf;
    private volatile Pattern tablasDeNegocio;

    public ConsultaSinNegocioInspector(ObjectProvider<EntityManagerFactory> emf) {
        this.emf = emf;
    }

    @Override
    public String inspect(String sql) {
        if (TenantContext.actual() == null && tablasDeNegocio().matcher(sql).find()) {
            log.warn("SQL sobre tabla de negocio sin negocio activo (tenant {}): {}", TenantResolver.SIN_NEGOCIO, sql);
        }
        return sql;
    }

    // Se arma en la primera consulta: al configurar Hibernate el metamodelo todavía no existe.
    private Pattern tablasDeNegocio() {
        if (tablasDeNegocio == null) {
            StringJoiner tablas = new StringJoiner("|", "\\b(", ")\\b");
            tablas.setEmptyValue("(?!)");
            emf.getObject().unwrap(SessionFactoryImplementor.class).getMappingMetamodel()
                    .forEachEntityDescriptor(entidad -> {
                        if (EntidadDeNegocio.class.isAssignableFrom(entidad.getMappedClass())) {
                            tablas.add(Pattern.quote(entidad.getMappedTableDetails().getTableName()));
                        }
                    });
            tablasDeNegocio = Pattern.compile(tablas.toString(), Pattern.CASE_INSENSITIVE);
        }
        return tablasDeNegocio;
    }

    @Override
    public void customize(Map<String, Object> props) {
        props.put(AvailableSettings.STATEMENT_INSPECTOR, this);
    }
}
