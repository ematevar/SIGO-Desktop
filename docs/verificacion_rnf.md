# Verificación de Requisitos No Funcionales (RNF-01 a RNF-11)

## 1. Entorno de Validación (RNF-01, RNF-02, RNF-03, RNF-04, RNF-10)
- **Sistema Operativo:** Windows 10 / 11 (64 bits).
- **JDK:** OpenJDK Temurin `26.0.2+10` (`--release 26`).
- **Construcción y Pruebas:** Apache Ant (`ant clean test jar`) + JUnit `4.13.2` (`14/14` casos de prueba exitosos).
- **Motor de Base de Datos:** MySQL 8 en la nube con conexión TLS (`sslMode=PREFERRED`), `connectTimeout=5000`, `socketTimeout=10000`, `useServerPrepStmts=true` y pool de conexiones reutilizables en `ClsConexionBD`.

## 2. Separación de Capas (RNF-05)
- La capa `Presentacion` **no** importa `ClsConexionBD` ni ejecuta sentencias SQL directas (verificado automáticamente por el test `testRnf05PresentacionSinAccesoDirectoABaseDeDatos`).
- Todas las reglas de negocio y validaciones de rol residen en la capa `Negocio` (`Cls*Service`, RNF-07).

## 3. Medición de Tiempos de Respuesta (RNF-06: ≤ 2 000 ms)

Tiempos medidos contra la instancia remota de MySQL 8 usando el pool de conexiones de `ClsConexionBD`:

| Operación / Consulta | Tiempo Medido (ms) | Límite SRS (ms) | Estado |
|---|---:|---:|:-:|
| Listado de Usuarios (RF-02) | 444 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Obras (RF-03) | 415 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Partidas (RF-04) | 402 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Materiales (RF-06) | 402 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Proveedores (RF-09) | 402 ms | 2 000 ms | ✅ Cumple |
| Catálogo de Personal (RF-10) | 402 ms | 2 000 ms | ✅ Cumple |
| Reporte 1 — Avance Curva S (RF-16a) | 637 ms | 2 000 ms | ✅ Cumple |
| Reporte 2 — Consumo vs. Presupuesto (RF-16b) | 1 009 ms | 2 000 ms | ✅ Cumple |
| Reporte 3 — Desempeño Personal (RF-16c) | 613 ms | 2 000 ms | ✅ Cumple |
| Reporte 4 — Estado Financiero (RF-16d) | 614 ms | 2 000 ms | ✅ Cumple |
| Reporte 1 filtrado por Obra y Fechas (RF-16) | 613 ms | 2 000 ms | ✅ Cumple |

## 4. Manejo de Excepciones y Registro Técnico (RNF-08)
- `ClsConstantes.mensajeUsuario(Throwable)` traduce errores de restricción `UNIQUE` (RUC, DNI, login, comprobante), claves foráneas, truncamiento y fallos de red en mensajes comprensibles sin exponer URLs, credenciales ni trazas SQL en pantalla.
- Los detalles técnicos se registran en el archivo local `sigoc_error.log`.
