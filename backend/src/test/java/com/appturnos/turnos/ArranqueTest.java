package com.appturnos.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ArranqueTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void arrancaYAplicaMigraciones() {
        Integer extensiones = jdbc.queryForObject(
                "select count(*) from pg_extension where extname = 'btree_gist'", Integer.class);
        assertThat(extensiones).isEqualTo(1);
    }
}
