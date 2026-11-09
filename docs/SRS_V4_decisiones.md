# Especificación de Requisitos de Software — SRS V4 y Decisiones de Arquitectura (SAD)

> **Propósito:** Documentar todas las precisiones funcionales, reglas de negocio, estructura de base de datos y decisiones de arquitectura implementadas en **SIGOC Desktop** para garantizar una trazabilidad del 100 % entre la documentación (SRS V4 / SAD) y el código fuente.

---

## 1. Correcciones de Dominio respecto al SRS V3

1. **Alcance y Proceso Propuesto (Obras Civiles):**
   - Se corrige cualquier mención errónea a *"gestión de reactivos"* en la sección de Proceso Propuesto y Objetos Entidad del borrador previo: el sistema gestiona exclusivamente **obras civiles, partidas presupuestales, almacén de materiales de construcción, proveedores, personal obrero, asistencia, producción diaria y finanzas por obra**.
2. **Naturaleza de la Aplicación (Viabilidad Técnica y RNF-01/RNF-02):**
   - Se corrige la referencia a *"aplicación web"* y *"navegador web"*: **SIGOC Desktop** es una aplicación nativa de escritorio en **Java 26 (`--release 26`) con interfaz gráfica Java Swing**, empaquetada mediante Apache Ant y conectada a **MySQL 8** vía JDBC/TLS.
3. **Unificación del Alcance con la Regla RF-03 (Baja / Cierre de Obras):**
   - El alcance general habla de mantenimiento de obras, pero la regla de negocio **RF-03** prohíbe eliminar obras que ya registren partidas o movimientos financieros/operativos. En **SRS V4** queda unificado:
     - Si una obra **no** tiene partidas ni movimientos, puede darse de baja lógicamente (`activo = 0`).
     - Si una obra **ya tiene** partidas o movimientos, el sistema **bloquea su baja** y ofrece la operación **Cerrar Obra** (`estado = 'Finalizada'`), conservando íntegro su historial para los reportes gerenciales.

---

## 2. Perfiles de Usuario y Matriz de Permisos (RF-01)

El sistema contempla únicamente **tres (3) perfiles de usuario** (se retira el rol `CONTADOR` y se estandariza `RESIDENTE` en lugar de `INGENIERO`):

| Módulo / Caso de Uso | Requisito | Administrador (`ADMIN`) | Residente de Obra (`RESIDENTE`) | Almacenero (`ALMACENERO`) |
|---|---|:---:|:---:|:---:|
| Iniciar / Cerrar Sesión y Cambiar Clave | RF-01 | ✔ | ✔ | ✔ |
| Gestionar Usuarios y Roles | RF-02 | ✔ | — | — |
| Gestionar Obras (Crear, Editar, Cerrar) | RF-03 | ✔ | — | — |
| Gestionar Partidas de Obra | RF-04 | ✔ | ✔ | — |
| Registrar Avance Físico de Partidas | RF-05 | *(Consulta en Reportes)* | ✔ *(Registro)* | — |
| Gestionar Catálogo de Materiales | RF-06 | ✔ | — | ✔ |
| Visualizar Alertas de Stock Mínimo | RF-06 / RF-08 | ✔ | — | ✔ |
| Registrar Compra de Materiales | RF-07 | ✔ | — | ✔ |
| Registrar Consumo de Materiales en Obra | RF-08 | — | ✔ | — |
| Gestionar Proveedores | RF-09 | ✔ | — | — |
| Gestionar Trabajadores (`empleado`) | RF-10 | ✔ | ✔ | — |
| Registrar Asistencia Diaria | RF-11 | — | ✔ | — |
| Registrar Producción Diaria por Partida | RF-12 | — | ✔ | — |
| Registrar Ingresos Financieros | RF-13 | ✔ | — | — |
| Generar y Cerrar Planilla | RF-14 | ✔ | — | — |
| Registrar Egresos Manuales (`Otro`) | RF-15 | ✔ | — | — |
| Generar y Exportar Reportes Gerenciales | RF-16 | ✔ | — | — |

> **Verificación en doble capa (RF-01, RNF-07):** Además de ocultar los módulos no autorizados en `FrmPrincipal`, cada método de `Cls*Service` valida explícitamente el rol de la sesión activa (`ClsConstantes.verificarRol`) y lanza `SecurityException` si un perfil no autorizado intenta invocarlo.

---

## 3. Definiciones Resueltas en el SRS V4 (Puntos `[SRS-?]`)

### 3.1. Definición de la Curva S Planificada vs. Ejecutada (RF-16a)
- **Avance Planificado (%):** Dado que el cronograma contractual de una obra civil se rige por las fechas maestras `obra.fecha_inicio_plan` y `obra.fecha_fin_plan`, el avance físico planificado acumulado a una fecha de corte $t$ se calcula mediante interpolación lineal acotada entre $0\,\%$ y $100\,\%$:
  $$\text{Avance Planificado}(t) = \min\left(100,\; \max\left(0,\; \frac{\text{días}(t - \text{fecha\_inicio\_plan})}{\text{días}(\text{fecha\_fin\_plan} - \text{fecha\_inicio\_plan})} \times 100\right)\right)$$
- **Avance Ejecutado Ponderado (%):** En cada fecha de corte $t$, para cada partida activa $p$ de la obra se toma el último `porcentaje_avance` registrado con `fecha_reporte <= t` (arrastrando el último valor reportado para evitar caídas artificiales a cero) y se pondera por su `peso_presupuestal`:
  $$\text{Avance Ejecutado}(t) = \frac{\sum_{p} \left(\text{peso\_presupuestal}_p \times \text{último\_avance}_p(t)\right)}{\sum_{p} \text{peso\_presupuestal}_p}$$

### 3.2. Asistencia Validada y Estados Pagados en Planilla (RF-11, RF-14)
- **Estados de asistencia permitidos:** `PRESENTE`, `TARDANZA`, `FALTA`, `PERMISO`.
- En los estados `FALTA` y `PERMISO`, el sistema deshabilita y limpia las horas de entrada y salida (`NULL` en base de datos). En `PRESENTE` y `TARDANZA`, exige hora de entrada y valida que `hora_salida > hora_entrada`.
- **Asistencia validada remunerada (RF-14):** Al ejecutar el cierre de planilla de una obra para un periodo `[desde, hasta]`, se consideran asistencias válidas remunerables aquellas con `activo = 1` y estado `PRESENTE` o `TARDANZA`.
- **Generación de egresos e inmutabilidad:**
  - El cierre calcula el monto devengado por cada trabajador ($\text{días asistidos} \times \text{jornal\_diario}$), inserta el registro maestro en `cierre_planilla` y genera en la **misma transacción JDBC** un registro en `egreso` con `tipo_egreso = 'Planilla'` por cada trabajador remunerado, vinculado mediante `id_cierre`.
  - Se rechaza cualquier intento de cerrar un periodo que se superponga con un cierre previo de la misma obra, así como cualquier intento de registrar asistencias cuya fecha caiga dentro de un periodo ya cerrado (**periodo cerrado inmutable**).

### 3.3. Presupuesto de Materiales por Partida y Desviaciones (RF-16b)
- Se incorpora la tabla `partida_material (id_partida_material, id_partida, id_material, cantidad_presupuestada, activo, fecha_borrado)` con restricción `UNIQUE(id_partida, id_material)` y `CHECK (cantidad_presupuestada > 0)`.
- El reporte **(b) Consumo real vs. presupuestado** calcula para cada material:
  - $\text{Desviación Absoluta} = \text{Consumo Real} - \text{Cantidad Presupuestada}$
  - $\text{Desviación (\%)} = \frac{\text{Consumo Real} - \text{Cantidad Presupuestada}}{\text{Cantidad Presupuestada}} \times 100$

---

## 4. Decisiones de Arquitectura — SAD V4 (RNF-05, RNF-09)

1. **Estructura de Paquetes en 4 Capas:**
   - Se mantienen los nombres de paquetes exactos definidos en el Diseño Arquitectónico del Sistema (SAD):
     - `Presentacion` (`FrmLogin`, `FrmPrincipal`, `FrmRegistros`, `FrmOperaciones`, `FrmReportes`, `FrmUsuarios`, `ClsPanelGrafico`, `ClsSwingUtil`)
     - `Negocio` (`ClsUsuarioService`, `ClsCatalogoService`, `ClsAvanceService`, `ClsMaterialService`, `ClsOperacionService`, `ClsReporteService`, `ClsCaptchaService`)
     - `NegocioDAO` (`ClsUsuarioDAO`, `ClsCatalogoDAO`, `ClsAvanceDAO`, `ClsMaterialDAO`, `ClsOperacionDAO`)
     - `Entidad` (`ClsEntidadBase`, `ClsUsuario`, `ClsRol`, `ClsObra`, `ClsPartida`, `ClsAvancePartida`, `ClsMaterial`, `ClsProveedor`, `ClsCompraMaterial`, `ClsDetalleCompra`, `ClsConsumoMaterial`, `ClsEmpleado`, `ClsAsistencia`, `ClsProduccionEmpleado`, `ClsCierrePlanilla`, `ClsIngreso`, `ClsEgreso`)
     - `config` (`ClsConexionBD`, `ClsConstantes`)
2. **Aislamiento Estricto de la Capa de Presentación (RNF-05):**
   - Ninguna clase de `Presentacion` importa ni invoca a `ClsConexionBD`. Toda consulta de historial y opciones pasa por `Negocio` → `NegocioDAO`.
3. **Uso de Entidades de Dominio (`Entidad`):**
   - Los DAOs (`ClsCatalogoDAO`, `ClsOperacionDAO`, `ClsUsuarioDAO`) instancian y pueblan las clases POJO del paquete `Entidad` heredadas de `ClsEntidadBase`, garantizando que las 17 clases del modelo de dominio formen parte activa del flujo de datos.
