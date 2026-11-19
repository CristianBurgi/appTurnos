package com.appturnos.turnos.plataforma.tenant;

import static com.appturnos.turnos.plataforma.tenant.TenantContext.comoNegocio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.appturnos.turnos.TestcontainersConfig;
import com.appturnos.turnos.negocio.Negocio;
import com.appturnos.turnos.negocio.NegocioRepository;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class AislamientoTest {

    private static final String AVISO = "sin negocio activo";

    @Autowired NegocioRepository negocios;
    @Autowired NotaPruebaRepository notas;

    @AfterEach
    void limpiar() {
        TenantContext.limpiar();
    }

    private Long nuevoNegocio() {
        String slug = "n-" + UUID.randomUUID();
        return negocios.save(new Negocio(slug, slug)).getId();
    }

    @Test
    void cadaNegocioVeSoloLoSuyo() {
        Long a = nuevoNegocio();
        Long b = nuevoNegocio();
        NotaPrueba deA = comoNegocio(a, () -> notas.save(new NotaPrueba("de A")));
        NotaPrueba deB = comoNegocio(b, () -> notas.save(new NotaPrueba("de B")));

        assertThat(deA.getNegocioId()).isEqualTo(a);
        assertThat(comoNegocio(a, () -> notas.findAll())).extracting(NotaPrueba::getTexto).containsExactly("de A");
        assertThat(comoNegocio(a, () -> notas.findById(deB.getId()))).isEmpty();
        assertThat(comoNegocio(b, () -> notas.findById(deA.getId()))).isEmpty();
    }

    @Test
    void sinNegocioNoSeVeNadaYQuedaLogueado(CapturedOutput salida) {
        Long a = nuevoNegocio();
        NotaPrueba deA = comoNegocio(a, () -> notas.save(new NotaPrueba("de A")));
        assertThat(salida).doesNotContain(AVISO);

        assertThat(notas.findAll()).isEmpty();
        assertThat(notas.findById(deA.getId())).isEmpty();
        assertThat(salida).contains("WARN").contains(AVISO);
    }

    @Test
    void sinNegocioNoSeGuardanDatosDeNegocio() {
        assertThatThrownBy(() -> notas.save(new NotaPrueba("huérfana")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasTablasDePlataformaNoNecesitanNegocio(CapturedOutput salida) {
        Long a = nuevoNegocio();

        assertThat(negocios.findById(a)).isPresent();
        assertThat(salida).doesNotContain(AVISO);
    }

    @Test
    void comoNegocioRestauraElContexto() {
        comoNegocio(1L, () -> null);

        assertThat(TenantContext.actual()).isNull();
    }
}
