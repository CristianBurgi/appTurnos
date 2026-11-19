package com.appturnos.turnos.plataforma.tenant;

import java.util.function.Supplier;

/**
 * Negocio activo en el hilo actual. Por request lo setea la sesión (paso 1.3);
 * en jobs se usa {@link #comoNegocio}, que siempre lo limpia.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> ACTUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Long actual() {
        return ACTUAL.get();
    }

    public static void set(Long negocioId) {
        ACTUAL.set(negocioId);
    }

    public static void limpiar() {
        ACTUAL.remove();
    }

    public static <T> T comoNegocio(Long negocioId, Supplier<T> accion) {
        Long anterior = ACTUAL.get();
        ACTUAL.set(negocioId);
        try {
            return accion.get();
        } finally {
            if (anterior == null) ACTUAL.remove(); else ACTUAL.set(anterior);
        }
    }
}
