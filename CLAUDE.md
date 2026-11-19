# CLAUDE.md — Gestor de turnos SaaS

Contexto para cualquier sesión de trabajo sobre este repo. Leer antes de tocar código.

## Fuente de verdad
- `ESPECIFICACION.md` en la raíz del repo.
- Si algo no está en la especificación o la contradice: **preguntar antes de implementar**.
- Cambiar una decisión de la especificación requiere explicar el porqué y esperar OK.

## Forma de trabajo (obligatoria)
- Fases con checkpoint: no se avanza de fase sin aprobación explícita.
- Pasos chicos; al cerrar cada paso, listar archivos creados/modificados con una línea cada uno, y esperar revisión.
- **Nada de commits ni push** (los hace el dueño con GitHub Desktop). **Un commit por paso aprobado**: al cerrar cada paso, sugerir el mensaje.
- Antes de cerrar fase: compilar y correr tests. Si algo falla, avisar y no darla por terminada.
- No sumar dependencias ni servicios pagos sin avisar el costo.
- Actualizar este archivo al cerrar cada fase.
- Código simple, mantenible por una sola persona. Sin sobreingeniería.

## Estado
- Fase 0 (arquitectura): **aprobada** 2026-11-19.
- **Fase actual: 1 — Base del proyecto, auth y roles, multi-tenant, esqueleto frontend, primer deploy.**

## Stack
| Capa | Elección | Por qué |
|---|---|---|
| Backend | Java 21 LTS + Spring Boot 4.1.x + Spring Security + Spring Data JPA (Hibernate 7) | Lo que el dueño domina. 4.2.0 salió el 2026-11-19: se evita por recién salida |
| Build | Maven vía wrapper (`backend/mvnw`, fija Maven 3.9.10) | El build no depende del Maven instalado en cada máquina |
| DB | PostgreSQL + Flyway, extensión `btree_gist` | Constraint de exclusión para cero solapamientos |
| Frontend | React + Vite + TypeScript + vite-plugin-pwa, en `frontend/`; el build lo sirve Spring Boot | Un solo deploy, mismo origen, sin CORS |
| Auth panel | Sesión con cookie httpOnly + CSRF, Spring Session JDBC, "recordarme" | Sobrevive redeploys; revocable |
| Cliente final | Sin cuenta: cookie firmada de dispositivo recordado + token por turno | Spec 5.4 |
| Email | API HTTP (Brevo recomendado, o Resend) detrás de `CanalEmail`, vía `RestClient` | Intercambiable, sin SDK, evita bloqueo de SMTP |
| Push | Web Push con VAPID (gratis) | Spec 5.7 |
| Jobs | `@Scheduled` en la app, idempotentes, outbox en DB | Sin servicio extra |
| Tests | JUnit 5 + Testcontainers (PostgreSQL real) | Exclusión y `@TenantId` no se prueban en H2 |
| Deploy | Railway: 1 servicio (Dockerfile multi-stage) + 1 PostgreSQL | Ya conocido |

## Decisiones de arquitectura
1. **Multi-tenant por columna `negocio_id`** con `@TenantId` de Hibernate. Schema-por-tenant descartado (migraciones × N, reportes globales, pool).
   - `@TenantId` no filtra SQL nativo ⇒ prohibido en repos de negocio. Única excepción permitida: SQL nativo en `plataforma.jobs`, con filtro explícito por `negocio_id` y comentado.
   - Suite de aislamiento: 2 negocios, acceso cruzado ⇒ 404.
   - Futuro opcional: Row Level Security.
2. **Tenant en jobs y superadmin**:
   - El resolver de tenant lee un `TenantContext` (por request: el negocio de la sesión; por job: el que se setea).
   - **Jobs recorren negocio por negocio**: setean el tenant, procesan, lo limpian (en `finally`). Además lo necesitan por la zona horaria de cada negocio.
   - **Superadmin** trabaja sobre tablas de plataforma sin `@TenantId` (`negocio`, `plan`, `suscripcion`, `usuario`). Para métricas globales sobre tablas de negocio usa el tenant **root** (`isRoot`) solo en consultas de lectura de un servicio dedicado.
   - Sin tenant seteado, nunca "ver todo". Hibernate 7 no abre sesiones sin tenant (ni para tablas de plataforma), así que `TenantResolver` usa el id inexistente `-1`: las consultas sobre entidades de negocio vuelven **vacías** y los inserts fallan por FK. Aprobado por el dueño (2026-11-19) con tres condiciones para que el vacío no esconda bugs:
     1. **Filtro web** (paso 1.3): toma el negocio de la sesión; un endpoint de negocio que llega sin negocio ⇒ error, nunca vacío. **Un test por cada endpoint de negocio**, además de los de aislamiento.
     2. **Jobs**: siempre negocio por negocio con `TenantContext.comoNegocio(id, () -> ...)`, que restaura el contexto.
     3. **WARN** de `ConsultaSinNegocioInspector` (hecho): todo SQL que toca una tabla de una `EntidadDeNegocio` sin negocio activo. Un WARN de esos = bug.
   - Toda entidad de negocio extiende `plataforma.tenant.EntidadDeNegocio` (`id` + `negocio_id` con `@TenantId`).
   - **Propuesta para Fase 2 (no implementada): consultas globales del superadmin.**
     - `TenantContext.comoPlataforma(() -> ...)` setea un marcador explícito `PLATAFORMA = 0L` (distinto del `-1` "sin negocio"). `TenantResolver.isRoot(0L) == true` ⇒ Hibernate no agrega el filtro por `negocio_id`.
     - Solo se usa dentro de un servicio dedicado (`suscripcion.MetricasGlobales`), con `@Transactional(readOnly = true)`, y solo para agregados (conteos, sumas por negocio). Nada de escrituras como root.
     - Un test lo vigila: ninguna clase fuera de ese servicio llama a `comoPlataforma` (con ArchUnit o un grep en un test, a decidir).
     - El WARN no salta como root, porque el contexto no está vacío: es acceso global intencional.
     - Lo descartado: JDBC/SQL nativo para métricas, porque rompe la regla de "sin SQL nativo en negocio" y duplica el mapeo.
3. **Persona global sin `negocio_id`**, accesible solo vía Ficha. La página pública no precarga datos de Persona sin dispositivo recordado. **El nombre tipeado en una reserva sin dispositivo recordado va a la Ficha, nunca a la Persona.** Persona solo la edita el propio cliente; lo que corrige el negocio va a la Ficha.
4. **Ocupación materializada en `turno_bloque`**: una fila por tramo OCUPADO, con columnas `inicio` y `fin` (fin = fin del tramo + margen) y una **columna generada** `rango tstzrange GENERATED ALWAYS AS (tstzrange(inicio, fin, '[)')) STORED` no mapeada en JPA. `EXCLUDE USING gist (profesional_id WITH =, rango WITH &&) WHERE (activo)`.
   - **El margen va después de CADA tramo OCUPADO** y queda dentro del rango, así el EXCLUDE también lo hace cumplir. En la tintura el hueco LIBRE pierde 5 min.
   - El margen del último tramo puede pasarse del fin del horario de trabajo.
5. **Turnos `PENDIENTE_APROBACION` crean bloques** y ocupan el horario mientras esperan. Al pasar a `RECHAZADO` o `NO_APROBADO` se desactivan los bloques.
6. **`NO_APROBADO`**: pendiente cuyo horario pasó sin aprobarse. Sin falta ni cobro.
7. **Profesional "cualquiera"**: se asigna al que tiene menos turnos ese día; empate ⇒ por `orden`. Si choca por concurrencia, se prueba el siguiente.
8. **Grilla**: cada 15 min (configurable por negocio). Además se ofrece como inicio posible el **fin de cada bloque ocupado (que ya incluye el margen)**, para no dejar tiempo muerto.
9. **Disponibilidad**: el servicio completo debe entrar en un mismo intervalo de trabajo (salvo el margen final); cada tramo OCUPADO + margen no puede superponerse con bloques activos.
10. **Recursos** (apagado en MVP): `SELECT … FOR UPDATE` sobre recursos en orden de id + conteo de usos.
11. **Cancelación/reprogramación por el cliente**:
    - Fuera de las 3 h: libre.
    - Dentro de las 3 h: **reprogramar bloqueado**; el link ofrece escribir por WhatsApp o cancelar avisando que cuenta como falta.
    - El profesional puede reprogramar a mano sin penalidad en cualquier momento.
12. **Al marcar falta** (hasta 48 h), en **una sola transacción**: turno ⇒ `FALTA`, cobro automático ⇒ `ANULADO`, movimiento `REVERSA` de fidelidad, ficha: `faltas+1`, se **recalculan** `visitas` y `ultima_visita` desde los turnos asistidos (no se resta a mano), y se evalúa si pasa a requerir aprobación.
13. **"Atendí sin turno"** = `turno` con origen `SIN_TURNO`, estado `ASISTIDO`, sin bloques.
14. **Fidelidad como libro de movimientos** (acumula / canje / reversa / ajuste).
15. **Recordatorios = filas de `envio` creadas al reservar** con `programado_para = inicio − horas`. **Si la reserva se hace con menos anticipación que esas horas (24 h por defecto), no se genera el recordatorio.** Cancelar/reprogramar los descarta. Despacho con `FOR UPDATE SKIP LOCKED`.
16. **Estado efectivo de suscripción calculado** con `Clock`; el job solo persiste para listados y avisos.
17. **Tiempo**: `timestamptz` en UTC; horarios `LocalTime` + zona del negocio; `Clock` inyectado.
18. **Valores de negocio en DB** (`negocio_config`, `plan`, `config_plataforma`), defaults de la spec en migraciones.
19. **Logo en PostgreSQL** (`bytea` con límite) en el MVP.
20. **Paquetes por funcionalidad**: `plataforma`, `suscripcion`, `negocio`, `catalogo`, `equipo`, `cliente`, `agenda`, `cobro`, `fidelidad`, `notificacion`, `publico`.

## Restricciones conocidas
- **Una sola réplica en Railway.** Los jobs no usan ShedLock: son idempotentes por cambio de estado y el despacho usa `SKIP LOCKED`, pero los jobs por hora (resumen, "volvé", cumpleaños) dependen de claves únicas. Antes de escalar a 2+ réplicas: agregar ShedLock o revisar cada job.
- App sleeping de Railway apagado (si no, los jobs no corren).
- Push en iPhone solo con PWA instalada (iOS 16.4+); email de respaldo.

## Estructura del repo
```
ESPECIFICACION.md
CLAUDE.md
backend/    Spring Boot (Maven)
frontend/   React + Vite (desde paso 1.4)
Dockerfile  multi-stage (desde paso 1.5)
```

## Plan de fases
0. Arquitectura — aprobada
1. Base, auth y roles, multi-tenant, esqueleto frontend, primer deploy ← **acá**
   - 1.1 Esqueleto backend (Maven, Flyway, `btree_gist`, `Clock`, health, test de arranque) — aprobado
   - 1.2 Multi-tenant (`negocio`, `TenantContext`, resolver, WARN sin negocio, tests de aislamiento) — aprobado
   - 1.3 Auth y roles (usuario, membresía, login/logout/me, sesión JDBC, CSRF, superadmin inicial)
   - 1.4 Esqueleto frontend (login + panel vacío + PWA)
   - 1.5 Dockerfile + deploy en Railway
2. Planes, suscripciones, panel superadmin
3. Negocio: marca, servicios con tramos, profesionales, horarios, bloqueos, reglas
4. Motor de disponibilidad y reservas
5. Página pública por slug + identificación
6. Asistencia, faltas, cobros, "Atendí sin turno", reportes
7. Notificaciones
8. Fidelización
9. Panel, métricas, PWA instalable
10. Checklist de producción

## Correr en local
Requisitos: Java 21 y Docker Desktop (los tests levantan PostgreSQL 17 con Testcontainers). Maven no hace falta: se usa el wrapper.

Tests (desde `backend/`):
```
.\mvnw.cmd test        # Windows
./mvnw test            # Linux/Mac
```

App contra el PostgreSQL 17 local (servicio `postgresql-x64-17`, puerto 5432, base `turnos`):
```powershell
Start-Service postgresql-x64-17          # si está detenido (consola como admin)
# la base se crea una sola vez:  psql -U postgres -p 5432 -c "CREATE DATABASE turnos"
$env:DB_URL      = "jdbc:postgresql://localhost:5432/turnos"
$env:DB_USER     = "postgres"
$env:DB_PASSWORD = "<tu contraseña>"
.\mvnw.cmd spring-boot:run
```
- Sin variables, `application.yml` usa `localhost:5432/turnos` con `postgres`/`postgres`.
- Flyway aplica las migraciones al arrancar. Health: `http://localhost:8080/actuator/health`.
- El PostgreSQL 18 local (puerto 5433) no se usa: Railway y los tests van con la 17.

## Pendientes
- Paso 1.3: filtro web que setea `TenantContext` desde la sesión, rechaza endpoints de negocio sin negocio y lo limpia al final de cada request (condición 1 de la decisión 2).
- Tests de aislamiento por HTTP (acceso cruzado ⇒ 404) cuando existan endpoints de negocio.
- Tenant root para métricas globales del superadmin: fase 2 (ver propuesta en la decisión 2).
