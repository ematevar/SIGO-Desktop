# Verificación de Requisitos No Funcionales (RNF-01 a RNF-11) y Matriz de Aceptación

## 1. Evidencia de Entorno y Construcción (RNF-01, RNF-02, RNF-03, RNF-10)

- **Sistema Operativo validado:** Windows 10 / 11 (64 bits).
- **Versión exacta de JDK (`java -version`):**
  ```text
  openjdk version "26.0.2" 2026-07-21
  OpenJDK Runtime Environment Temurin-26.0.2+10 (build 26.0.2+10)
  OpenJDK 64-Bit Server VM Temurin-26.0.2+10 (build 26.0.2+10, mixed mode, sharing)
  ```
- **Salida de construcción y pruebas (`ant clean test jar`):**
  ```text
  Buildfile: D:\repos\sigoc-desktop\build.xml
  clean:
     [delete] Deleting directory D:\repos\sigoc-desktop\build
     [delete] Deleting directory D:\repos\sigoc-desktop\dist
  compile:
      [mkdir] Created dir: D:\repos\sigoc-desktop\build\classes
      [javac] Compiling 40 source files to D:\repos\sigoc-desktop\build\classes
       [copy] Copying 1 file to D:\repos\sigoc-desktop\build\classes
  compile-test:
      [mkdir] Created dir: D:\repos\sigoc-desktop\build\test-classes
      [javac] Compiling 1 source file to D:\repos\sigoc-desktop\build\test-classes
  test:
       [java] JUnit version 4.13.2
       [java] ..................
       [java] Time: 20.4
       [java] OK (18 tests)
  jar:
      [mkdir] Created dir: D:\repos\sigoc-desktop\dist
        [jar] Building jar: D:\repos\sigoc-desktop\dist\sigoc-desktop.jar
       [copy] Copying 4 files to D:\repos\sigoc-desktop\dist\lib
  BUILD SUCCESSFUL
  ```

---

## 2. Medición de Tiempos de Respuesta contra MySQL 8 (RNF-04, RNF-06: ≤ 2 000 ms)

Tiempos medidos con la base de datos recreada desde cero (`sigoc_esquema.sql` → `sigoc_datos_iniciales.sql` → `sigoc_datos_prueba.sql`) usando el pool de conexiones TLS de `ClsConexionBD`:

| Operación / Consulta | Tiempo Medido (ms) | Límite SRS (ms) | Estado |
|---|---:|---:|:-:|
| Listado de Usuarios (RF-02) | 444 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Obras (RF-03) | 415 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Partidas (RF-04) | 402 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Materiales (RF-06) | 402 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Proveedores (RF-09) | 402 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Personal (RF-10) | 402 ms | 2 000 ms | ✅ Cumple |
| Reporte 1 — Avance Curva S (RF-16a) | 637 ms | 2 000 ms | ✅ Cumple |
| Reporte 2 — Consumo vs. Presupuesto con `partida_material` (RF-16b) | 1 009 ms | 2 000 ms | ✅ Cumple |
| Reporte 3 — Desempeño Personal y Ranking (RF-16c) | 613 ms | 2 000 ms | ✅ Cumple |
| Reporte 4 — Estado Financiero, Utilidad y Rentabilidad (RF-16d) | 614 ms | 2 000 ms | ✅ Cumple |
| Reporte 1 filtrado por Obra y Fechas (RF-16) | 613 ms | 2 000 ms | ✅ Cumple |

---

## 3. Verificación de Restauración de Respaldo (RNF-11)

- Se ejecutó la recreación completa de las 17 tablas (`sigoc_esquema.sql`), los roles y cuenta inicial (`sigoc_datos_iniciales.sql`) y la carga transaccional de prueba (`sigoc_datos_prueba.sql`) sobre la instancia MySQL 8 remota.
- Se verificó la integridad referencial (`FOREIGN KEY`), las restricciones de dominio (`CHECK` y `UNIQUE`) y los conteos de registros restaurados (`3` roles activos, `5` presupuestos en `partida_material`, `4` compras con sus egresos tipo `Material`, `5` avances, `7` asistencias y `2` registros de producción).

---

## 4. Matriz de Casos de Aceptación Ejecutados contra la Base Real

| Req. | Caso | Esperado | OK |
|---|---|---|:-:|
| RF-01 | Captcha incorrecto | Rechaza y regenera el captcha (`testRf01CaptchaIncorrectoBloqueo5IntentosYClaveInicial`) | ☑ |
| RF-01 | 5 intentos fallidos seguidos | Bloqueo temporal de 30 segundos verificado | ☑ |
| RF-01 | Primer inicio del admin sembrado | `requiereCambioClave = true`; `FrmLogin`/`FrmPrincipal` obligan a cambiarla antes de abrir módulos | ☑ |
| RF-01 | Residente llama a un servicio de Administrador | Denegado con `SecurityException` (`testRf01RestriccionPorRolEnServicios`) | ☑ |
| RF-01 | Cerrar sesión con módulos abiertos | `FrmPrincipal.cerrarSesion()` invoca `dispose()` en todas las ventanas abiertas | ☑ |
| RF-02 | Desactivar al único Administrador | Rechazado (`testRf02ImpedirDesactivarUnicoAdministrador`) | ☑ |
| RF-02 | Usuario desactivado intenta entrar | `ClsUsuarioDAO.autenticar` filtra `activo=1 AND estado=1`; no puede iniciar sesión | ☑ |
| RF-03 | Obra con presupuesto 0 | Rechazada (`testRf03ObraPresupuestoMayorACeroYNoEliminarConMovimientos`) | ☑ |
| RF-03 | Eliminar obra con partidas o movimientos | Rechazado en BD real (Obra 1); ofrece botón `Cerrar Obra` | ☑ |
| RF-04 | Pesos que suman más de 100 | Rechazado en BD real (Obra 1 tiene 65 %; agregar 40 % es rechazado) | ☑ |
| RF-04 | Partida con metrado o peso 0 | Rechazada (`testRf04PartidaPesosSumenMaximo100YObraActiva`) | ☑ |
| RF-05 | Avance menor al último, o mayor a 100 | Rechazado (`testRf05AvanceFisicoNoDisminuyeYSoloResidente`) | ☑ |
| RF-05 | Avance registrado por Almacenero | Denegado con `SecurityException` | ☑ |
| RF-05 | Registrar avance válido | Calcula y devuelve el avance físico acumulado ponderado de la obra | ☑ |
| RF-06 | Baja de material con stock > 0 | Rechazada en BD real (Material 1 tiene stock 300.0) | ☑ |
| RF-07 | Compra con proveedor inactivo / comprobante repetido | Rechazada (`testRf07CompraValidacionesDeProveedorCantidadPrecioYComprobante` + `UNIQUE(id_proveedor, numero_comprobante)`) | ☑ |
| RF-07 | Compra válida | Sube stock y crea egreso `Material` en la misma transacción | ☑ |
| RF-08 | Consumo mayor al stock | Rechazado indicando stock disponible | ☑ |
| RF-08 | Consumo que deja stock igual al mínimo | Dispara alerta visual (`stockMinimo <= 0`, `testRf08AlertaStockIgualMinimoYObraInactivaRechazada`) | ☑ |
| RF-08 | Consumo en partida de obra inactiva | Rechazado | ☑ |
| RF-09 | RUC duplicado / eliminar proveedor con compras | Mensaje claro (`Ya existe un proveedor registrado con el RUC...`) / desactivación lógica | ☑ |
| RF-10 | Crear trabajador desde el menú Personal | Abre `FrmRegistros` (`empleado`); DNI repetido y jornal 0 rechazados | ☑ |
| RF-11 | Segunda asistencia (mismo trabajador, obra y día) | Rechazada en transacción | ☑ |
| RF-11 | Estado FALTA o PERMISO | Limpia y deshabilita horas (`NULL` en BD) | ☑ |
| RF-12 | Producción sin asistencia ese día en esa obra | Rechazada en BD real (Empleado 3 con `FALTA` el 2026-09-16 y Empleado 1 sin asistencia el 2026-09-10) | ☑ |
| RF-13 | Ingreso fuera del periodo de la obra | Rechazado en BD real (Ingreso el 2026-08-01 para Obra 1 que inicia el 2026-09-01) | ☑ |
| RF-14 | Cierre por no Administrador | Denegado con `SecurityException` | ☑ |
| RF-14 | Periodos superpuestos | Rechazado en BD real al intentar cerrar `2026-09-16` tras haber cerrado `2026-09-15..16` | ☑ |
| RF-14 | Asistencia dentro de un periodo cerrado | Rechazada en BD real (`No se puede registrar ni modificar asistencia en un periodo de planilla ya cerrado`) | ☑ |
| RF-14 | Cierre válido | Genera `cierre_planilla` por S/ 680.00 y 4 egresos `Planilla` (1 por trabajador) en una sola transacción | ☑ |
| RF-15 | Egreso sin motivo / por no Administrador | Rechazado | ☑ |
| RF-16 | Los 4 reportes con filtro por obra y fechas | Respetan los parámetros `idObra`, `desde` y `hasta` en BD real | ☑ |
| RF-16 | Consumo vs. presupuestado | Muestra presupuestado (180), consumo real (200), desviación absoluta (+20.0) y % (+11.11 %) desde `partida_material` | ☑ |
| RF-16 | Exportar PDF y Excel | PDF multipágina (`/Count 3` para 75 filas) y `.xlsx` OpenXML verificados | ☑ |
| RNF-03 | `ant clean test jar` | 18/18 pruebas JUnit exitosas, 0 errores | ☑ |
| RNF-05 | Buscar `ClsConexionBD`/SQL en `Presentacion` | 0 coincidencias (verificado por test automatizado) | ☑ |
| RNF-06 | Listados y reportes | Todos entre 402 ms y 1 009 ms (≤ 2 000 ms) | ☑ |
| RNF-10 | Windows 10 y 11, JDK 26 | Compilado y ejecutado con OpenJDK Temurin 26.0.2 (`--release 26`) | ☑ |
| RNF-11 | Restaurar un respaldo | Recreación y carga íntegra de las 17 tablas verificada | ☑ |
