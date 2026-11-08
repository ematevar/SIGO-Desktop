# SIGOC Desktop — Sistema de Gestión de Obras Civiles

Fase de escritorio desarrollada en **Java Swing**, **JDK 26**, **Apache Ant** y **MySQL 8** mediante JDBC, siguiendo la arquitectura en capas impuesta por el SRS (**Presentación**, **Negocio / NegocioDAO**, **Entidad** y **Config**, RNF-05).

## Requisitos

- **JDK 26** (se compila con `--release 26` en Windows 10/11, RNF-01 / RNF-10).
- **Apache Ant**.
- **MySQL 8** accesible desde el equipo (RNF-04).
- Archivos JAR requeridos en `lib/`:
  - `lib/mysql-connector-j.jar` — Driver JDBC para MySQL 8.
  - `lib/jcalendar-1.4.jar` — Selector visual de fechas (`JDateChooser`).
  - `lib/junit-4.13.2.jar` y `lib/hamcrest-core-1.3.jar` — Ejecución de pruebas automatizadas (`ant test`, RNF-03).
- Clientes `mysql` y `mysqldump` para carga de esquemas y respaldos (RNF-11).

## Preparación de MySQL

1. En el servidor MySQL, autoriza la conexión y utiliza un usuario de aplicación con permisos sobre `sigoc_db`.
2. Desde la raíz del repositorio, carga el esquema, los datos iniciales y (opcionalmente) los datos de prueba:

   ```powershell
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_esquema.sql
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_datos_iniciales.sql
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_datos_prueba.sql
   ```

3. Copia `config.properties.example` como `config.properties` en la raíz del proyecto (o en el directorio de trabajo desde donde ejecutes `dist/sigoc-desktop.jar`) y completa `db.host`, `db.port`, `db.name`, `db.user`, `db.password` y `db.ssl`. `config.properties` está ignorado por Git.

## Perfiles y Roles del Sistema (RF-01)

El sistema implementa los tres perfiles definidos en el SRS:

| Perfil SRS | Código en BD | Módulos y Operaciones Permitidas |
|---|---|---|
| **Administrador** | `ADMIN` | Usuarios (RF-02), Obras (RF-03), Partidas (RF-04), Materiales (RF-06), Proveedores (RF-09), Personal (RF-10), Finanzas: Ingresos, Cierre de Planilla y Egresos Otro (RF-13, RF-14, RF-15), Reportes Gerenciales (RF-16) |
| **Residente de Obra** | `RESIDENTE` | Partidas (RF-04), Avances físicos y avance acumulado de obra (RF-05), Consumos de materiales (RF-08), Personal (RF-10), Asistencia diaria (RF-11) y Producción por partida (RF-12) |
| **Almacenero** | `ALMACENERO` | Catálogo de materiales y alertas de stock mínimo (RF-06, RF-08), Compras de materiales (RF-07) |

## Compilar, Probar y Ejecutar (RNF-03)

```powershell
ant clean
ant compile
ant test
ant jar
ant run
```

- `ant test` compila y ejecuta la suite JUnit (`Negocio.SigocSrsComplianceTest`) que verifica las reglas de negocio de RF-01 a RF-16 y RNF-05.
- `ant jar` empaqueta `dist/sigoc-desktop.jar` y copia las librerías de `lib/` a `dist/lib/`.
- `ant run` inicia `FrmLogin`.

## Cumplimiento de Módulos y Reglas de Negocio

- **Seguridad (RF-01, RF-02):** Autenticación PBKDF2-HMAC-SHA256, captcha regenerado en cada intento fallido, bloqueo temporal tras intentos fallidos consecutivos, cambio obligatorio de la contraseña inicial en el primer inicio de sesión, cierre automático de todas las ventanas de módulos al cerrar sesión, e imposibilidad de desactivar al único Administrador activo.
- **Obras y Partidas (RF-03, RF-04):** Presupuesto mayor que cero (`> 0`), validación de fechas coherentes, bloqueo de eliminación de obras con partidas o movimientos (permitiendo cerrarlas/finalizarlas), y validación en partidas de obra activa con metrado `> 0`, peso `> 0` y suma de pesos por obra `≤ 100 %`.
- **Avances Físicos (RF-05):** Exclusivo para Residente de Obra sobre obras activas; impide disminuir el porcentaje acumulado o registrar fechas regresivas y calcula/muestra automáticamente el avance físico acumulado ponderado de la obra.
- **Materiales, Compras y Consumos (RF-06, RF-07, RF-08):** `stock_actual` de solo lectura, bloqueo de baja de materiales con `stock_actual > 0`, compras con validación de proveedor y obra activos y generación automática del egreso tipo `Material`, y consumos con validación de stock suficiente, cálculo del consumo acumulado por partida y alerta cuando `stock_resultante <= stock_minimo`.
- **Proveedores y Personal (RF-09, RF-10, RF-11, RF-12):** Validación de RUC (11 dígitos) y DNI (8 dígitos) únicos con mensajes claros, módulo `PERSONAL` para el CRUD de trabajadores con `jornal_diario > 0`, módulo `ASISTENCIA` (1 registro por trabajador + obra + día, salida > entrada y limpieza automática de horas en `FALTA`/`PERMISO`), y módulo `PRODUCCION` que exige asistencia válida del trabajador ese mismo día en la obra de la partida.
- **Finanzas y Planilla (RF-13, RF-14, RF-15):** Ingresos validados dentro del rango de ejecución de la obra activa, cierre de planilla exclusivo del Administrador que bloquea periodos superpuestos, vuelve inmutable la asistencia del periodo cerrado y genera los egresos tipo `Planilla` en una sola transacción, y egresos manuales tipo `Otro` con motivo obligatorio.
- **Reportes Gerenciales (RF-16):** 4 reportes de solo lectura filtrables por **obra y rango de fechas**, exportables a **PDF (`.pdf`), Excel (`.xlsx`) y CSV (`.csv`)**:
  1. Avance físico planificado vs. ejecutado (Curva S).
  2. Consumo de materiales real vs. presupuestado con desviaciones absoluta y porcentual.
  3. Desempeño del personal (% de asistencia/cumplimiento, producción total y ranking de productividad promedio).
  4. Estado financiero por obra (ingresos, egresos desglosados por Material, Planilla y Otro, utilidad neta y % de rentabilidad).

## Rendimiento y Respaldo (RNF-06, RNF-11)

- Consulta el registro de tiempos de respuesta (`< 2 s`) y compatibilidad en [docs/verificacion_rnf.md](docs/verificacion_rnf.md).
- Consulta el procedimiento de respaldo y restauración en [docs/backup.md](docs/backup.md).
