# SIGOC Desktop — Sistema de Gestión de Obras Civiles

Fase de escritorio en Java Swing, JDK 26, Apache Ant y MySQL 8 mediante JDBC. Las capas están separadas en `entidad`, `negocio`, `negocio.dao`, `presentacion` y `config`.

## Requisitos

- JDK 26 (se compila con `--release 26`).
- Apache Ant.
- Instancia MySQL 8 accesible desde el equipo.
- MySQL Connector/J dentro de `lib/mysql-connector-j.jar`.
- Cliente `mysql` para cargar el esquema y cliente `mysqldump` para respaldos.

## Preparación de MySQL

1. En el proveedor, permite la IP pública del equipo en el firewall y usa un usuario SQL de aplicación con permisos limitados.
2. Desde la raíz de este repositorio, carga el esquema y luego los datos iniciales:

   ```powershell
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_esquema.sql
   mysql -h HOST -P 3306 -u USUARIO -p < sql/sigoc_datos_iniciales.sql
   ```

   El archivo de esquema reproduce las 16 tablas suministradas. `CREATE TABLE IF NOT EXISTS` permite ejecutarlo en la instancia que ya tenga esas tablas; no migra columnas de esquemas distintos.
3. Copia `config.properties.example` como `config.properties` en la raíz del proyecto y completa host, puerto, base, usuario, contraseña y TLS. `config.properties` está ignorado por Git.
4. Descarga MySQL Connector/J y coloca el JAR en `lib/`. El driver no se incluye en el repositorio.

El script de datos crea la cuenta temporal `admin` con contraseña `Admin123!`. Inicia sesión con captcha y cámbiala desde **Sesión → Cambiar contraseña** antes de cargar información real.

## Compilar y ejecutar

```powershell
ant clean
ant compile
ant jar
ant run
```

`ant jar` genera `dist/sigoc-desktop.jar` y copia el driver a `dist/lib`. `ant run` abre `FrmLogin`. La primera prueba de conexión es el propio inicio de sesión; ante un error muestra la causa y conserva la aplicación abierta.

## Módulos

- **Seguridad:** autenticación PBKDF2, captcha regenerado tras cada intento, sesión y menús por rol. Administración permite crear, editar, activar/desactivar usuarios y cambiar la contraseña propia.
- **Obras y partidas:** mantenimiento validado de obras y partidas.
- **Avances:** porcentaje acumulado entre 0 y 100, fechas no regresivas y registro del usuario responsable.
- **Materiales y almacén:** catálogo con stock de solo lectura, compras con detalle, incremento de stock y egreso Material atómicos; consumo con bloqueo del stock, validación antes del descuento y alerta de mínimo.
- **Proveedores:** mantenimiento con validación de RUC.
- **Personal:** mantenimiento, asistencia diaria y producción por partida.
- **Finanzas:** ingresos, egresos manuales Otro y cierre de planilla desde las asistencias presentes. El cierre crea su egreso Planilla en la misma transacción y registra al usuario que lo ejecuta.
- **Reportes:** avance ejecutado por las fechas registradas, consumo acumulado y stock, asistencia/productividad y saldo financiero por obra. Cada vista se exporta a CSV compatible con Excel.

Las validaciones de negocio están en `Cls*Service`; la interfaz solo recopila datos y presenta resultados. Las escrituras de compra, consumo y planilla usan transacciones JDBC.

El modelo recibido no incluye un calendario de avance planificado ni cantidades de material presupuestadas. Por ese motivo, usando únicamente estas 16 tablas, el reporte no puede calcular curva S planificado vs. ejecutado ni desviación de consumo contra presupuesto; presenta los datos reales que sí están guardados.

La exportación se entrega en CSV, formato que abre Excel. La prueba de rendimiento inferior a dos segundos y la validación de compatibilidad en Windows 10/11 requieren datos y acceso al servidor del proveedor; deben medirse en la instancia final.

## Git y seguridad

El repositorio ignora `config.properties`, JARs descargados, artefactos de compilación y respaldos. Usa commits descriptivos y nunca incluyas credenciales ni dumps en el control de versiones.

## Respaldo

Consulta [docs/backup.md](docs/backup.md). Verifica la política de backups automáticos del proveedor y genera un dump manual antes de cada entrega. Conserva el dump fuera del repositorio.
