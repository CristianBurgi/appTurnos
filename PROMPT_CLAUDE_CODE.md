# CONTEXTO Y ROL
Actuá como arquitecto de software y desarrollador senior full-stack. Soy desarrollador Java con experiencia intermedia en Spring Boot. Ya desplegué proyectos en producción con PostgreSQL y Railway, y uso GitHub Desktop.

Vamos a construir un SaaS multi-tenant de gestión automática de turnos, vendido por suscripción mensual. Arranca con peluquerías y barberías y tiene que poder escalar a otros rubros sin rehacer el núcleo.

# FUENTE DE VERDAD
Antes de hacer nada, leé `docs/ESPECIFICACION.md`. Ahí están el producto completo, las reglas, los casos límite y lo que queda fuera del MVP.
- Si algo no está en la especificación o se contradice con ella, preguntame antes de implementar.
- Si en algún momento proponés cambiar una decisión de la especificación, explicame por qué y esperá mi OK.

Las decisiones de estructura más importantes, que no se pueden perder:
- Multi-tenant con aislamiento total entre negocios.
- Cliente en dos niveles: una Persona global (por WhatsApp) y una Ficha por negocio (historial, fidelidad, notas, faltas).
- Servicios divididos en tramos OCUPADO/LIBRE. El profesional solo queda bloqueado en los tramos OCUPADO.
- Recursos físicos incluidos en el modelo, pero apagados en el MVP.
- Turno asistido automáticamente al pasar su horario, con el cobro registrado al precio de lista. El profesional solo marca faltas (hasta 48 h después) y corrige cobros.
- Capa de notificaciones desacoplada: email automático, links wa.me con el texto listo y push de la PWA.
- Planes y suscripciones con límites validados en el backend. Activación manual en el MVP; Mercado Pago queda preparado.
- Todo valor de negocio es configurable, no fijo en el código.

# FORMA DE TRABAJO (obligatoria)
Voy a revisar cada cambio en mi IDE antes de seguir.
- Trabajá por fases con checkpoints. No avances a la fase siguiente sin mi aprobación explícita.
- Dentro de cada fase, avanzá en pasos chicos y revisables.
- Al terminar cada paso, listá los archivos creados y modificados, con una línea sobre qué cambió en cada uno.
- No hagas commits ni push: los hago yo con GitHub Desktop. Al cerrar cada fase, sugerime el mensaje de commit.
- Antes de cerrar una fase, compilá y corré los tests. Si algo falla, avisame y no lo des por terminado.
- Mantené en la raíz un archivo `CLAUDE.md` con el contexto, el stack, las decisiones tomadas y su porqué, la fase actual y los pendientes. Actualizalo al cerrar cada fase.
- No sumes dependencias ni servicios pagos sin avisarme el costo.

En cada fase:
1. Explicá qué vas a hacer y las decisiones técnicas, con alternativas y el porqué.
2. Implementá solo lo de esa fase.
3. Cerrá con un resumen: qué quedó hecho, cómo probarlo, qué queda pendiente y qué necesitás que decida.

# FASE 0: Arquitectura (sin código, salvo el CLAUDE.md)
1. Stack completo. Backend en Spring Boot + PostgreSQL. Frontend PWA: dame 2 o 3 opciones con pros y contras, pensando en que lo mantiene una sola persona.
2. Estrategia multi-tenant: compará "tenant_id en cada tabla" contra "schema por tenant" y recomendá una.
3. Modelo de datos completo (entidades, relaciones y campos clave). Incluí Persona/Ficha, servicios por tramos, recursos, turnos, cobros, fidelidad, plantillas, envíos y planes/suscripciones.
4. Algoritmo del motor de disponibilidad con tramos y recursos, y cómo evitar solapamientos bajo concurrencia.
5. Tareas programadas en Railway: recordatorios por email, marcado automático de asistencia, armado de "Mensajes de hoy", avisos de "volvé" y cumpleaños, vencimiento de suscripciones.
6. Proveedor de email y de push web, con su costo.
7. Estructura de paquetes y plan de fases.

# FASES SIGUIENTES (ajustá el orden en la Fase 0)
1. Base del proyecto, autenticación y roles, multi-tenant.
2. Planes, suscripciones y panel de superadmin (alta manual de negocios).
3. Negocio: marca, servicios con tramos, profesionales, horarios y reglas.
4. Motor de disponibilidad y reservas: confirmación automática o manual, faltas, turnos cargados a mano. Testeá a fondo.
5. Página pública de reservas por slug, con identificación por WhatsApp y email opcional con código.
6. Asistencia automática, faltas, cobros, "Atendí sin turno" y reportes de ingresos.
7. Notificaciones: emails, plantillas, links de WhatsApp, "Mensajes de hoy" y push.
8. Fidelización: tarjeta de visitas o puntos, "volvé", cumpleaños y canjes.
9. Panel, métricas y PWA instalable.
10. Deploy en Railway y checklist de producción.

# ANTES DE RESPONDER CADA FASE
Revisá que lo propuesto cumpla los criterios de calidad de la especificación y que no bloquee lo que quedó fuera del MVP: otros rubros, recursos, autoregistro, Mercado Pago y la API de WhatsApp.

Empezá por la Fase 0.
