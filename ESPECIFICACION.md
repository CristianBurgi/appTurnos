# Especificación del producto — Gestor de turnos (MVP escalable)

> Fuente de verdad del producto. Toda decisión técnica debe respetar este documento.
> Si algo no está acá o se contradice, preguntar antes de implementar.

Última actualización: 2026-11-19

---

## 1. Objetivo

SaaS multi-tenant de gestión automática de turnos, vendido por **suscripción mensual** a los negocios.

- Rubro inicial: peluquerías y barberías.
- A futuro: otros rubros (estética, uñas, consultorios, etc.) **sin rehacer el núcleo**.

Problemas que resuelve (relevados con el cliente piloto):
1. El profesional tiene que contestar WhatsApp mientras trabaja.
2. Clientes que no vienen y no avisan.

Valor agregado para vender la suscripción:
- Fidelización de clientes (que vuelvan).
- Reportes de ingresos semanales, quincenales y mensuales.

### Cliente piloto
Una peluquería atendida por su dueño (1 solo profesional). Hoy maneja los turnos por WhatsApp.

---

## 2. Usuarios y roles

| Rol | Qué hace |
|---|---|
| **Superadmin** (dueño de la plataforma) | Da de alta negocios, gestiona planes, suscripciones y métricas globales |
| **Dueño del negocio** | Configura negocio, marca, servicios, profesionales, horarios, reglas y plantillas |
| **Profesional** | Gestiona su agenda desde el celular, marca faltas, registra cobros |
| **Cliente final** | Reserva, reprograma o cancela desde un link web, sin crear cuenta |

En el piloto, dueño y profesional son la misma persona (un usuario puede tener ambos roles).

---

## 3. Plataforma

- **PWA** (web instalable) para el panel del negocio/profesional.
- **Web responsive** para la página pública de reservas (se entra por link o QR).
- Sin app nativa en el MVP.
- Backend: Spring Boot + PostgreSQL. Deploy en Railway.

---

## 4. Modelo conceptual (clave para escalar)

### 4.1 Multi-tenant
- Cada **Negocio** es un tenant. Sus datos están totalmente aislados.
- Ningún negocio puede ver datos de otro bajo ninguna circunstancia.

### 4.2 Identidad del cliente: dos niveles
- **Persona (global):** identificada por su número de WhatsApp normalizado. Contiene nombre, email (opcional), cumpleaños (opcional), estado de verificación.
- **Ficha de cliente (por negocio):** vínculo Persona ↔ Negocio. Contiene historial de visitas, fidelidad, notas del profesional, contador de faltas, marca de "requiere aprobación".
- Una misma persona puede atenderse en varios negocios. **Ningún negocio ve la actividad de la persona en otro negocio.**

### 4.3 Servicios por tramos
- Un servicio tiene precio y una secuencia de **tramos**, cada uno con duración y tipo:
  - `OCUPADO`: el profesional está trabajando con el cliente.
  - `LIBRE`: el cliente sigue en el local pero el profesional puede atender a otro (ej. pose de tintura).
- Ejemplo — Tintura: 15 min OCUPADO → 30 min LIBRE → 20 min OCUPADO.
- Un corte simple es un único tramo OCUPADO.
- El motor de disponibilidad bloquea al profesional **solo en los tramos OCUPADO**.

### 4.4 Recursos físicos (preparado, apagado en MVP)
- Entidad **Recurso** (lavacabezas, sillón, camilla) con capacidad.
- Un tramo puede requerir uno o más recursos.
- El motor debe poder validar disponibilidad de recursos además del profesional.
- En el MVP la funcionalidad existe en el modelo pero está deshabilitada por configuración (y es del plan Pro).

### 4.5 Genericidad para otros rubros
- Evitar nombres de dominio atados a peluquería en el núcleo (usar "profesional", "servicio", "recurso", "turno").
- El rubro del negocio es un dato configurable, no lógica fija.

---

## 5. Funcionalidades del MVP

### 5.1 Alta de negocios
- MVP: el superadmin crea el negocio desde su panel y le da acceso al dueño.
- Preparado (apagado): autoregistro del dueño con prueba gratis.

### 5.2 Marca del negocio
- MVP: **logo, colores principales y nombre**.
- Página pública de reservas por slug: `app.com/<slug-negocio>` + QR descargable.
- Preparado para después: portada, texto de presentación, dirección/mapa, Instagram, dominio propio.

### 5.3 Agenda y reservas
- Servicios con tramos y precio.
- Horarios por profesional, días libres, bloqueos, feriados.
- El cliente elige: servicio → profesional (o "cualquiera") → horario disponible.
- Solo se muestran huecos realmente libres.
- **Nunca** dos turnos superpuestos para el mismo profesional (ni con reservas simultáneas → control de concurrencia en el backend).
- El profesional puede **cargar turnos a mano** (para clientes que siguen escribiendo por WhatsApp).
- Botón **"Atendí sin turno"**: registra servicio + cobro en dos toques; cliente opcional (si se indica, suma a su fidelidad).

#### Reglas de reserva (valores por defecto, configurables por negocio)
| Regla | Valor por defecto |
|---|---|
| Anticipación mínima para reservar | 2 horas |
| Máximo hacia adelante | 30 días |
| Cancelar/reprogramar sin penalidad | hasta 3 horas antes |
| Cancelación tardía | cuenta como falta |
| Margen entre turnos | 5 minutos |

#### Confirmación de reservas
- Por defecto: **confirmación automática**.
- Opción por negocio: "aprobar reservas a mano".
- Cliente con **N faltas** (N configurable por negocio) → sus reservas en ese negocio pasan a requerir aprobación.

### 5.4 Identificación del cliente al reservar
- Datos obligatorios: nombre + WhatsApp.
- Email **opcional**.
  - Con email: código de verificación por email **solo en la primera reserva**; luego el dispositivo queda recordado. Recibe confirmación y recordatorios automáticos por email.
  - Sin email: puede reservar igual; el turno queda marcado **"sin verificar"** y su recordatorio va por WhatsApp desde el panel.
- Número normalizado a formato internacional (Argentina: `549` + característica + número). Números inválidos se rechazan o avisan.

### 5.5 Asistencia y faltas
- Al pasar el horario del turno, se marca **asistido automáticamente**.
- El profesional **solo marca las faltas**.
- Ventana de corrección: puede marcar falta hasta **48 h** después del turno.
- Las faltas se cuentan **por negocio** (nunca globales).

### 5.6 Cobros e ingresos
- Al marcarse asistido, se **registra el cobro automáticamente** con el precio de lista del servicio.
- El profesional puede editar monto y medio de pago (efectivo, transferencia, Mercado Pago, otro).
- Cobro en el local, no online.
- Reportes: **semanal, quincenal y mensual**, con desglose por servicio y por medio de pago.

### 5.7 Notificaciones y mensajes
Capa de notificaciones **desacoplada** (canales intercambiables: email, WhatsApp link, push; a futuro API oficial de WhatsApp).

**Al cliente:**
- Email automático: confirmación de reserva y recordatorio antes del turno (por defecto 24 h, configurable), con link para confirmar/cancelar/reprogramar.
- WhatsApp **sin API paga**: la app genera links `https://wa.me/<numero>?text=<mensaje>` con el texto listo. El profesional toca y envía.

**Panel "Mensajes de hoy":** lista de envíos pendientes (recordatorios, "volvé", cumpleaños, aviso de premio), cada uno con botón de WhatsApp y marca de "enviado".

**Plantillas:** sugeridas por defecto y **editables por cada negocio**. Variables:
`{nombre}`, `{servicio}`, `{profesional}`, `{fecha}`, `{hora}`, `{negocio}`, `{link_turno}`.
El texto debe codificarse correctamente para URL (acentos, saltos de línea, emojis).

Plantilla sugerida de recordatorio:
> Hola {nombre}! Te recuerdo tu turno {fecha} a las {hora} para {servicio} en {negocio}. Si no podés venir avisame acá: {link_turno}

**Al profesional:**
- Notificaciones **push (PWA)**: reserva nueva, cancelación, reprogramación.
- **Resumen diario por email** con la agenda del día.

### 5.8 Fidelización
- Historial de visitas por cliente (por negocio).
- Motor de fidelidad único con reglas configurables. Cada negocio elige:
  - **Tarjeta de visitas** (ej. cada 6 cortes, el 7.º gratis), o
  - **Puntos** (según monto cobrado; se canjean por premios definidos por el negocio).
- El **canje siempre lo confirma el negocio** en el local.
- Aviso **"volvé"**: cuando pasan X días desde la última visita (configurable por negocio o por servicio) → aparece en "Mensajes de hoy".
- **Cumpleaños**: saludo con beneficio opcional.

### 5.9 Suscripciones y planes
| | Básico | Pro |
|---|---|---|
| Profesionales | 1 | hasta 5 |
| Agenda + página de reservas con marca | ✔ | ✔ |
| Recordatorios email + links WhatsApp | ✔ | ✔ |
| Registro de cobros y reportes de ingresos | ✔ | ✔ |
| Fidelización | — | ✔ |
| Métricas avanzadas | — | ✔ |
| Recursos físicos | — | ✔ |

- **Prueba gratis:** 30 días con plan Pro.
- Precios **editables desde el panel de superadmin** (referencia inicial ≈ $30.000/mes, a validar).
- Estados: `PRUEBA`, `ACTIVA`, `VENCIDA`, `SUSPENDIDA`.
- Al vencer: días de gracia (configurable) → luego se bloquean **reservas nuevas**; el negocio conserva acceso a sus datos.
- Límites del plan validados **en el backend**.
- MVP: activación/renovación **manual** por el superadmin.
- Preparado para después: cobro automático con Mercado Pago (suscripciones / preapproval).

### 5.10 Panel del negocio
Agenda del día y semana, clientes, servicios, mensajes de hoy, configuración, estado de la suscripción, ingresos y métricas (turnos, cancelaciones, faltas, clientes que volvieron).

---

## 6. Fuera del MVP (no implementar, pero no bloquear)
- Autoregistro de negocios (preparado, apagado).
- Recursos físicos activos (preparado, apagado).
- Cobro automático de suscripciones con Mercado Pago.
- Señas / pagos online de clientes.
- API oficial de WhatsApp.
- App móvil nativa.
- Múltiples sucursales por negocio.
- Marca extendida y dominio propio.
- Otros rubros.

---

## 7. Excepciones y casos límite
- Cliente sin email → turno "sin verificar", recordatorio por WhatsApp desde el panel.
- Cliente que escribe por WhatsApp → el profesional carga el turno a mano.
- Cliente sin turno → "Atendí sin turno".
- Dos clientes reservan el mismo hueco a la vez → solo uno se confirma.
- Falta no marcada a tiempo → corregible hasta 48 h.
- Cancelación tardía → cuenta como falta.
- Suscripción vencida → gracia → bloqueo de reservas nuevas, datos intactos.
- Número de teléfono mal cargado → normalización y aviso.
- Cambio de horario del profesional con turnos ya tomados → avisar los turnos afectados, no borrarlos.
- Zona horaria: `America/Argentina/Buenos_Aires` por defecto, configurable por negocio.

---

## 8. Criterios de calidad
- Cero turnos superpuestos, también bajo concurrencia.
- Aislamiento total entre negocios.
- Límites de plan validados en backend.
- Zona horaria correcta en reservas, recordatorios y reportes.
- Reserva completa desde el celular en menos de 1 minuto.
- Tests en: disponibilidad (incluye tramos), reservas, estados de suscripción, asistencia/faltas, cobros y armado de links de WhatsApp.
- Ningún valor de negocio fijo en el código (tiempos, reglas, límites, precios).
- Código simple y mantenible por una sola persona. Sin sobreingeniería.

---

## 9. Riesgos y pendientes
- **Precio final:** validar con costos (Railway, dominio, envío de emails) y con el piloto.
- **Push en iPhone:** solo funciona con la PWA instalada (iOS 16.4+). El email queda como respaldo.
- **Hábito del profesional:** si no marca faltas, se ensucian fidelidad y métricas.
- **Proveedor de email:** elegir uno con plan gratis suficiente para el MVP.
