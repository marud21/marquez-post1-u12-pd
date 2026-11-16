# Reembolsos Integrado — Proyecto Integrador

**Universidad Francisco de Paula Santander — Ingeniería de Sistemas**
**Curso:** Patrones de Diseño de Software (1156701-A) — Post-contenido Unidad 12
**Estudiante:** Diego Armando Márquez Rigaud — **Código:** 1152115

Sistema de solicitudes de reembolso de gastos corporativos que reemplaza la hoja de cálculo compartida del
área de Finanzas.

## Diseño Propuesto

Escrito **antes** de implementar cualquier patrón, a partir de los tres requisitos funcionales.

| Requisito | Qué varía (punto de variabilidad) | Patrón propuesto | Justificación (test de explicabilidad) |
|---|---|---|---|
| **RF1** — Validación en varias etapas | El **conjunto y el orden** de verificaciones que debe superar una solicitud (hoy tope por categoría, documentos soporte y duplicados; mañana un límite anual acumulado) | **Chain of Responsibility** | Cada verificación es un eslabón independiente que rechaza o pasa la solicitud al siguiente, así que agregar una verificación es crear un eslabón y enlazarlo, sin tocar los existentes. |
| **RF2** — Cálculo distinto por categoría | La **fórmula** del monto aprobado, elegida una sola vez por la categoría (tope diario × días, monto hasta tope mensual, 80 % hasta tope anual; mañana "bienestar") | **Strategy** (+ registro de estrategias por categoría) | Cada categoría calcula el mismo tipo de resultado con un algoritmo propio e intercambiable, sin estado ni orden entre ellas, que es exactamente lo que encapsula Strategy. |
| **RF3** — Notificación simultánea a varios interesados | **Quién** reacciona a un cambio de estado y **cómo** (empleado: siempre; Contabilidad: solo aprobaciones; auditoría: todo; mañana un panel de métricas) | **Observer** | El servicio publica "la solicitud cambió de estado" a una lista de observadores con reacciones propias, de modo que sumar un interesado no modifica al servicio ni a los demás. |

Descartados en este análisis: **State** para el ciclo de vida de la solicitud (los requisitos no definen
transiciones con reglas propias entre estados, solo notificar el cambio) y **Template Method** para las
calculadoras (no comparten un esqueleto de algoritmo, solo la firma).

Modelo de dominio (`dominio/`): `SolicitudReembolso`, `CategoriaGasto`, `EstadoSolicitud`, sin imports de
Spring ni de JPA.

## Decisiones de diseño (Parte 1)

1. **Orden de la cadena: documentos → monto → duplicados.** El eslabón más caro es `ValidadorDuplicados`
   (recorre la lista de pendientes que el servicio cargó de la base de datos); los otros dos son
   comparaciones O(1) sobre la propia solicitud. Ubicándolo **último**, una solicitud sin soportes o con un
   monto fuera de tope se rechaza sin recorrer la lista. Se pierde solo que, si hay varios problemas, el
   empleado ve primero el más barato de detectar y no el de duplicado (la cadena reporta el primer rechazo).
2. **Selección de calculadora con un mapa construido desde `List<CalculadoraReembolso>`** (inyectada por
   Spring con todos los `@Component`), no con un `switch` manual. RF2 exige agregar categorías sin modificar
   el código existente: con el mapa, "bienestar" es una clase nueva `CalculadoraBienestar` y una constante
   en `CategoriaGasto`; ni `SelectorCalculadora` ni las otras calculadoras cambian. El `switch` obligaría a
   editar el selector en cada categoría nueva (y el compilador no avisa si se olvida un caso con `default`).

Pruebas: 20 (validadores, cadena, calculadoras, selector, notificadores y 2 de integración
`@SpringBootTest` con MockMvc, H2 y `@SpyBean` sobre los tres observadores).

## Qué se descartó y por qué

Criterio (guía, sección 4.2): ¿la variabilidad que justificaría la alternativa es real y documentada hoy, o
es especulación sobre un futuro que aún no llegó?

1. **`ApplicationEventPublisher` + `@EventListener` en vez de `List<ObservadorCambioEstado>` inyectada.**
   Con tres observadores fijos y conocidos en compilación, un bus de eventos genérico no aporta
   desacoplamiento real (ya lo da la interfaz) y sí dificulta rastrear quién reacciona a qué cambio.
   *Reconsiderar si:* un observador necesita ejecutarse después del commit o de forma asíncrona
   (`@TransactionalEventListener`), por ejemplo con la API de Contabilidad.
2. **Visitor o Abstract Factory para exportar reportes en varios formatos.** Solo existe el requisito de CSV;
   no hay una segunda solicitud real (no se cumple la Rule of Three). *Reconsiderar si:* Contabilidad pide
   formalmente un segundo y un tercer formato.
3. **(Propio) Una jerarquía de excepciones por validador** (`DocumentosFaltantesException`,
   `MontoExcedidoException`, `SolicitudDuplicadaException`) en lugar de `ResultadoValidacion` + una sola
   `SolicitudInvalidaException` con el motivo. Se consideró para que el controlador devolviera códigos
   distintos por tipo de rechazo. Se descartó: hoy los tres rechazos se tratan igual (HTTP 400 con un mensaje
   para el empleado) y nadie consume el tipo de error; tres clases más solo moverían el texto del motivo a
   nombres de clase, y además obligarían a los eslabones a lanzar excepciones como control de flujo, rompiendo
   el contrato "aprobar o pasar" de la cadena. *Reconsiderar si:* un cliente (por ejemplo, el frontend o la API
   de Contabilidad) necesita reaccionar programáticamente distinto según el motivo; entonces bastaría agregar
   un código enumerado a `ResultadoValidacion`, sin jerarquía de excepciones.
4. **(Propio) Un puerto `RepositorioSolicitudes` "por simetría" con Hexagonal.** Es la alternativa evaluada en
   ADR-001: sin un cambio de motor previsto, sería una interfaz con una sola implementación posible.
   *Reconsiderar si:* aparece una segunda fuente de datos o una migración de motor.
5. **(Propio) Anotar el dominio con `@Entity`.** Era lo más rápido para que `JpaRepository` funcionara, pero
   habría roto el requisito de un dominio sin JPA; se resolvió con `META-INF/orm.xml`, que no agrega
   indirección en el código Java.
