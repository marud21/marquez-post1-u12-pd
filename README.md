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
