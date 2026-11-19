package com.appturnos.turnos.plataforma;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Todo lo que dependa de "ahora" recibe este Clock, para poder fijar la hora en los tests. */
@Configuration
public class TiempoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
