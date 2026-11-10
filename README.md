# SIGOC Desktop — Sistema de Gestión de Obras Civiles

Fase de escritorio desarrollada en **Java Swing**, **JDK 26**, **Apache Ant** y **MySQL 8** mediante JDBC, siguiendo la arquitectura en capas impuesta por el SRS (**Presentación**, **Negocio / NegocioDAO**, **Entidad** y **Config**, RNF-05).

## Requisitos

- **JDK 26** (se compila con `--release 26` en Windows 10/11, RNF-01 / RNF-10).
- **Apache Ant**.
- **MySQL 8** accesible desde el equipo (RNF-04).
- Archivos JAR requeridos en `lib/` (consulta [lib/README.md](lib/README.md)):
  - `lib/mysql-connector-j.jar` — Driver JDBC oficial para MySQL 8 (descargable desde Maven Central `com.mysql:mysql-connector-j` o dev.mysql.com).
  - `lib/jcalendar-1.4.jar` — Selector visual de fechas `JDateChooser` (`com.toedter:jcalendar:1.4`).
  - `lib/junit-4.13.2.jar` y `lib/hamcrest-core-1.3.jar` — Ejecución de pruebas automatizadas (`ant test`, incluidos en Apache NetBeans o Maven Central `junit:junit:4.13.2`).
- Clientes `mysql` y `mysqldump` para carga de esquemas y respaldos (RNF-11).

## Preparación y Recreación de MySQL

1. En el servidor MySQL, autoriza la conexión y utiliza un usuario de aplicación con permisos sobre `sigoc_db`.
2. **Importante al actualizar desde una versión previa:** Dado que el esquema incorpora nuevas restricciones (`CHECK`, `UNIQUE`), la tabla `partida_material` y la estandarización de roles (`RESIDENTE` en lugar de `INGENIERO`), **debes respaldar ([docs/backup.md](docs/backup.md)) y recrear la base de datos** antes de ejecutar los scripts en este orden exacto:

   ```powershell
   mysql -h HOST -P 3306 -u USUARIO -p -e "DROP DATABASE IF EXISTS sigoc_db;"
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_esquema.sql
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_datos_iniciales.sql
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_datos_prueba.sql
   ```

3. **Ubicación de `config.properties`:** Copia `config.properties.example` como `config.properties` en el **directorio de trabajo desde donde ejecutes la aplicación** (la raíz del repositorio si usas `ant run` o `java -jar dist/sigoc-desktop.jar` desde la raíz). Completa `db.host`, `db.port`, `db.name`, `db.user`, `db.password` y `db.ssl`. `config.properties` está ignorado por Git.
4. En el primer inicio de sesión con la cuenta inicial sembrada (`admin`), el sistema **obligará a cambiar la contraseña inicial** antes de abrir el panel principal (`FrmPrincipal`).

## Perfiles y Roles del Sistema (RF-01 y SRS V4)

El sistema implementa los tres perfiles definidos en el SRS (documentados al detalle en [docs/SRS_V4_decisiones.md](docs/SRS_V4_decisiones.md)):

| Perfil SRS | Código en BD | Módulos y Operaciones Permitidas |
|---|---|---|
| **Administrador** | `ADMIN` | Usuarios (RF-02), Obras (RF-03), Partidas (RF-04), Materiales y alertas de stock (RF-06), Compras (RF-07), Proveedores (RF-09), Personal (RF-10), Finanzas: Ingresos, Cierre de Planilla y Egresos Otro (RF-13, RF-14, RF-15), Reportes Gerenciales (RF-16) |
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

- `ant test` compila y ejecuta la suite JUnit (`Negocio.SigocSrsComplianceTest`, 18 pruebas unitarias y de integración contra MySQL) que verifica todas las reglas de negocio de RF-01 a RF-16 y RNF-05.
- `ant jar` empaqueta `dist/sigoc-desktop.jar` y copia las librerías de `lib/` a `dist/lib/`.
- `ant run` inicia `FrmLogin`.

## Documentación Técnica y Evidencias

- **Decisiones del SRS V4 y Arquitectura SAD:** [docs/SRS_V4_decisiones.md](docs/SRS_V4_decisiones.md).
- **Verificación de RNF, Tiempos (< 2 s) y Matriz de Aceptación:** [docs/verificacion_rnf.md](docs/verificacion_rnf.md).
- **Procedimiento de Respaldo y Restauración (RNF-11):** [docs/backup.md](docs/backup.md).
