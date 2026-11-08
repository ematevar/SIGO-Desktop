package NegocioDAO;

import config.ClsConexionBD;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Operaciones transaccionales de compras, asistencia, producción, ingresos, egresos y planilla
 * (RF-07, RF-11, RF-12, RF-13, RF-14, RF-15, RNF-05).
 */
public class ClsOperacionDAO {

    private static final Set<String> TABLAS_HISTORIAL = Set.of(
            "avance_partida", "compra_material", "consumo_material",
            "asistencia", "produccion_empleado", "ingreso", "egreso", "cierre_planilla"
    );

    /**
     * Registra una compra de material en una transacción atómica verificando que el proveedor,
     * la obra y el material estén activos, el comprobante no esté duplicado para ese proveedor,
     * incrementando el stock y creando automáticamente el egreso tipo {@code Material} (RF-07).
     *
     * @param proveedor ID del proveedor.
     * @param obra ID de la obra destino.
     * @param material ID del material comprado.
     * @param tipoComprobante Factura o Boleta.
     * @param comprobante Número de comprobante.
     * @param fecha Fecha de la compra.
     * @param cantidad Cantidad comprada (> 0).
     * @param precio Costo unitario (> 0).
     * @throws Exception Si alguna entidad está inactiva o ocurre un error SQL.
     */
    public void registrarCompra(int proveedor, int obra, int material, String tipoComprobante, String comprobante, LocalDate fecha, double cantidad, double precio) throws Exception {
        BigDecimal cantBd = BigDecimal.valueOf(cantidad).setScale(2, RoundingMode.HALF_UP);
        BigDecimal precioBd = BigDecimal.valueOf(precio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBd = cantBd.multiply(precioBd).setScale(2, RoundingMode.HALF_UP);

        transaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM proveedor WHERE id_proveedor=? FOR UPDATE")) {
                ps.setInt(1, proveedor);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("activo") != 1) {
                        throw new IllegalArgumentException("El proveedor seleccionado no existe o se encuentra inactivo (RF-07).");
                    }
                }
            }
            validarObraActivaEnConexion(c, obra);
            try (PreparedStatement dup = c.prepareStatement("SELECT 1 FROM compra_material WHERE id_proveedor=? AND LOWER(numero_comprobante)=LOWER(?) AND activo=1")) {
                dup.setInt(1, proveedor);
                dup.setString(2, comprobante);
                try (ResultSet rs = dup.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("Ya existe una compra registrada con el comprobante " + comprobante + " para este proveedor.");
                    }
                }
            }
            int compra;
            try (PreparedStatement p = c.prepareStatement("INSERT INTO compra_material(id_proveedor, tipo_comprobante, fecha_compra, numero_comprobante, total) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, proveedor);
                p.setString(2, tipoComprobante);
                p.setDate(3, java.sql.Date.valueOf(fecha));
                p.setString(4, comprobante);
                p.setBigDecimal(5, totalBd);
                p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) {
                    k.next();
                    compra = k.getInt(1);
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO detalle_compra(id_compra, id_material, cantidad, precio_unitario, subtotal) VALUES(?,?,?,?,?)")) {
                p.setInt(1, compra);
                p.setInt(2, material);
                p.setBigDecimal(3, cantBd);
                p.setBigDecimal(4, precioBd);
                p.setBigDecimal(5, totalBd);
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement("UPDATE material SET stock_actual=stock_actual+?, costo_unitario_ref=? WHERE id_material=? AND activo=1")) {
                p.setBigDecimal(1, cantBd);
                p.setBigDecimal(2, precioBd);
                p.setInt(3, material);
                if (p.executeUpdate() != 1) {
                    throw new IllegalArgumentException("El material seleccionado no existe o está inactivo.");
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO egreso(id_obra, fecha, tipo_egreso, concepto, monto, id_compra) VALUES(?,?,'Material',?,?,?)")) {
                p.setInt(1, obra);
                p.setDate(2, java.sql.Date.valueOf(fecha));
                p.setString(3, "Compra " + tipoComprobante + " " + comprobante);
                p.setBigDecimal(4, totalBd);
                p.setInt(5, compra);
                p.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Registra la asistencia diaria de un trabajador activo en una obra activa,
     * bloqueando duplicados por (empleado + obra + fecha) y rechazando registros en
     * periodos de planilla ya cerrados (RF-11, RF-14).
     *
     * @param empleado ID del trabajador.
     * @param obra ID de la obra.
     * @param fecha Fecha de asistencia.
     * @param entrada Hora de entrada (HH:MM:SS) o vacío en FALTA/PERMISO.
     * @param salida Hora de salida (HH:MM:SS) o vacío en FALTA/PERMISO.
     * @param estado Estado de asistencia (PRESENTE, TARDANZA, FALTA, PERMISO).
     * @throws Exception Si el periodo está cerrado, ya existe asistencia o el trabajador/obra está inactivo.
     */
    public void registrarAsistencia(int empleado, int obra, LocalDate fecha, String entrada, String salida, String estado) throws Exception {
        String sql = "INSERT INTO asistencia(id_empleado, id_obra, fecha, hora_entrada, hora_salida, estado) VALUES(?,?,?,?,?,?)";
        transaccion(c -> {
            try (PreparedStatement lock = c.prepareStatement("SELECT activo FROM empleado WHERE id_empleado=? FOR UPDATE")) {
                lock.setInt(1, empleado);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next() || rs.getInt("activo") != 1) {
                        throw new IllegalArgumentException("El trabajador seleccionado no existe o está inactivo (RF-10).");
                    }
                }
            }
            validarObraActivaEnConexion(c, obra);
            try (PreparedStatement cerrado = c.prepareStatement("SELECT 1 FROM cierre_planilla WHERE id_obra=? AND activo=1 AND ? BETWEEN fecha_inicio_periodo AND fecha_fin_periodo LIMIT 1")) {
                cerrado.setInt(1, obra);
                cerrado.setDate(2, java.sql.Date.valueOf(fecha));
                try (ResultSet rs = cerrado.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("No se puede registrar ni modificar asistencia en un periodo de planilla ya cerrado (RF-14).");
                    }
                }
            }
            try (PreparedStatement exists = c.prepareStatement("SELECT 1 FROM asistencia WHERE id_empleado=? AND id_obra=? AND fecha=? AND activo=1")) {
                exists.setInt(1, empleado);
                exists.setInt(2, obra);
                exists.setDate(3, java.sql.Date.valueOf(fecha));
                try (ResultSet rs = exists.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("Ya existe asistencia registrada para este trabajador, obra y fecha (RF-11).");
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setInt(1, empleado);
                p.setInt(2, obra);
                p.setDate(3, java.sql.Date.valueOf(fecha));
                p.setTime(4, entrada == null || entrada.isBlank() ? null : java.sql.Time.valueOf(entrada));
                p.setTime(5, salida == null || salida.isBlank() ? null : java.sql.Time.valueOf(salida));
                p.setString(6, estado);
                p.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Registra la producción diaria de un trabajador verificando que la partida y su obra estén activas,
     * y que el trabajador tenga asistencia registrada (PRESENTE o TARDANZA) ese mismo día en esa misma obra (RF-12).
     *
     * @param empleado ID del trabajador.
     * @param partida ID de la partida.
     * @param fecha Fecha de producción.
     * @param cantidad Cantidad producida (> 0).
     * @throws Exception Si la partida no está activa o el trabajador no asistió a esa obra ese día.
     */
    public void registrarProduccion(int empleado, int partida, LocalDate fecha, double cantidad) throws Exception {
        transaccion(c -> {
            int idObra;
            String sqlPartida = "SELECT p.id_obra, p.activo AS p_activo, o.activo AS o_activo, o.estado "
                    + "FROM partida p JOIN obra o ON o.id_obra=p.id_obra WHERE p.id_partida=?";
            try (PreparedStatement ps = c.prepareStatement(sqlPartida)) {
                ps.setInt(1, partida);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("p_activo") != 1) {
                        throw new IllegalArgumentException("La partida seleccionada no existe o está inactiva (RF-12).");
                    }
                    String estadoObra = rs.getString("estado");
                    if (rs.getInt("o_activo") != 1
                            || (estadoObra != null && (estadoObra.equalsIgnoreCase("Finalizada")
                            || estadoObra.equalsIgnoreCase("Inactiva")
                            || estadoObra.equalsIgnoreCase("Cerrada")))) {
                        throw new IllegalArgumentException("La obra a la que pertenece la partida no se encuentra activa (RF-12).");
                    }
                    idObra = rs.getInt("id_obra");
                }
            }
            String sqlAsistencia = "SELECT estado FROM asistencia WHERE id_empleado=? AND id_obra=? AND fecha=? AND activo=1";
            try (PreparedStatement ps = c.prepareStatement(sqlAsistencia)) {
                ps.setInt(1, empleado);
                ps.setInt(2, idObra);
                ps.setDate(3, java.sql.Date.valueOf(fecha));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("El trabajador no tiene asistencia registrada en la obra de esta partida para la fecha " + fecha + " (RF-12).");
                    }
                    String est = rs.getString(1);
                    if (est == null || (!est.equalsIgnoreCase("PRESENTE") && !est.equalsIgnoreCase("TARDANZA") && !est.equalsIgnoreCase("ASISTIO") && !est.equalsIgnoreCase("ASISTIÓ"))) {
                        throw new IllegalArgumentException("No se puede registrar producción porque el trabajador figura con estado '" + est + "' en esa fecha (RF-12).");
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO produccion_empleado(id_empleado, id_partida, fecha, cantidad_producida) VALUES(?,?,?,?)")) {
                p.setInt(1, empleado);
                p.setInt(2, partida);
                p.setDate(3, java.sql.Date.valueOf(fecha));
                p.setBigDecimal(4, BigDecimal.valueOf(cantidad).setScale(2, RoundingMode.HALF_UP));
                p.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Registra un ingreso financiero validando que la obra esté activa y que la fecha del ingreso
     * se encuentre dentro del periodo de ejecución planificado de la obra (RF-13).
     *
     * @param obra ID de la obra.
     * @param fecha Fecha del ingreso.
     * @param concepto Concepto o motivo del ingreso.
     * @param monto Monto positivo (> 0).
     * @throws Exception Si la obra no está activa o la fecha está fuera del periodo de ejecución.
     */
    public void registrarIngreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception {
        BigDecimal montoBd = BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP);
        transaccion(c -> {
            LocalDate[] rango = validarObraActivaEnConexion(c, obra);
            if (fecha.isBefore(rango[0]) || fecha.isAfter(rango[1])) {
                throw new IllegalArgumentException("La fecha del ingreso (" + fecha + ") debe estar dentro del periodo de ejecución de la obra (" + rango[0] + " al " + rango[1] + ") (RF-13).");
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO ingreso(id_obra, fecha, concepto, monto) VALUES(?,?,?,?)")) {
                p.setInt(1, obra);
                p.setDate(2, java.sql.Date.valueOf(fecha));
                p.setString(3, concepto);
                p.setBigDecimal(4, montoBd);
                p.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Registra un egreso manual tipo {@code Otro} validando que la obra esté activa (RF-15).
     *
     * @param obra ID de la obra.
     * @param fecha Fecha del egreso.
     * @param concepto Concepto o motivo obligatorio.
     * @param monto Monto positivo (> 0).
     * @throws Exception Si la obra no está activa o ocurre un error SQL.
     */
    public void registrarEgreso(int obra, LocalDate fecha, String concepto, double monto) throws Exception {
        BigDecimal montoBd = BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP);
        transaccion(c -> {
            validarObraActivaEnConexion(c, obra);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO egreso(id_obra, fecha, tipo_egreso, concepto, monto) VALUES(?,?,'Otro',?,?)")) {
                p.setInt(1, obra);
                p.setDate(2, java.sql.Date.valueOf(fecha));
                p.setString(3, concepto);
                p.setBigDecimal(4, montoBd);
                p.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Ejecuta el cierre de planilla de una obra para un periodo de fechas en una única transacción:
     * impide cierres con periodos superpuestos, calcula los jornales de los trabajadores con asistencia
     * válida (PRESENTE o TARDANZA), registra el cierre y genera los egresos tipo {@code Planilla}
     * vinculados a dicho cierre (RF-14).
     *
     * @param obra ID de la obra.
     * @param desde Fecha inicial del periodo.
     * @param hasta Fecha final del periodo.
     * @param usuario ID del Administrador que ejecuta el cierre.
     * @return Monto total de la planilla cerrada.
     * @throws Exception Si el periodo se superpone con un cierre previo o no hay asistencias a pagar.
     */
    public double cerrarPlanilla(int obra, LocalDate desde, LocalDate hasta, int usuario) throws Exception {
        return transaccion(c -> {
            validarObraActivaEnConexion(c, obra);
            String sqlSolape = "SELECT fecha_inicio_periodo, fecha_fin_periodo FROM cierre_planilla "
                    + "WHERE id_obra=? AND activo=1 AND NOT (fecha_fin_periodo < ? OR fecha_inicio_periodo > ?) LIMIT 1 FOR UPDATE";
            try (PreparedStatement ps = c.prepareStatement(sqlSolape)) {
                ps.setInt(1, obra);
                ps.setDate(2, java.sql.Date.valueOf(desde));
                ps.setDate(3, java.sql.Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("El periodo seleccionado se superpone con un cierre de planilla ya registrado ("
                                + rs.getDate(1) + " al " + rs.getDate(2) + ") (RF-14).");
                    }
                }
            }

            String q = "SELECT e.id_empleado, e.nombres_apellidos, COUNT(*) AS dias, SUM(e.jornal_diario) AS subtotal "
                    + "FROM asistencia a JOIN empleado e ON e.id_empleado=a.id_empleado "
                    + "WHERE a.id_obra=? AND a.activo=1 AND e.activo=1 "
                    + "AND a.fecha BETWEEN ? AND ? "
                    + "AND UPPER(a.estado) IN ('PRESENTE','TARDANZA','ASISTIO','ASISTIÓ') "
                    + "GROUP BY e.id_empleado, e.nombres_apellidos";

            record PagoTrabajador(int idEmpleado, String nombre, int dias, BigDecimal subtotal) {}
            List<PagoTrabajador> pagos = new ArrayList<>();
            BigDecimal totalBd = BigDecimal.ZERO;

            try (PreparedStatement p = c.prepareStatement(q)) {
                p.setInt(1, obra);
                p.setDate(2, java.sql.Date.valueOf(desde));
                p.setDate(3, java.sql.Date.valueOf(hasta));
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        BigDecimal sub = r.getBigDecimal("subtotal").setScale(2, RoundingMode.HALF_UP);
                        if (sub.compareTo(BigDecimal.ZERO) > 0) {
                            pagos.add(new PagoTrabajador(r.getInt("id_empleado"), r.getString("nombres_apellidos"), r.getInt("dias"), sub));
                            totalBd = totalBd.add(sub);
                        }
                    }
                }
            }
            if (pagos.isEmpty() || totalBd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("No hay asistencias válidas para cerrar en ese periodo.");
            }
            int cierre;
            try (PreparedStatement p = c.prepareStatement("INSERT INTO cierre_planilla(id_obra, fecha_inicio_periodo, fecha_fin_periodo, monto_total, id_usuario) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, obra);
                p.setDate(2, java.sql.Date.valueOf(desde));
                p.setDate(3, java.sql.Date.valueOf(hasta));
                p.setBigDecimal(4, totalBd);
                p.setInt(5, usuario);
                p.executeUpdate();
                try (ResultSet r = p.getGeneratedKeys()) {
                    r.next();
                    cierre = r.getInt(1);
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO egreso(id_obra, fecha, tipo_egreso, concepto, monto, id_cierre) VALUES(?,?,'Planilla',?,?,?)")) {
                for (PagoTrabajador pago : pagos) {
                    p.setInt(1, obra);
                    p.setDate(2, java.sql.Date.valueOf(hasta));
                    String concepto = "Planilla " + desde + " al " + hasta + " — " + pago.nombre() + " (" + pago.dias() + "d)";
                    if (concepto.length() > 150) {
                        concepto = concepto.substring(0, 150);
                    }
                    p.setString(3, concepto);
                    p.setBigDecimal(4, pago.subtotal());
                    p.setInt(5, cierre);
                    p.addBatch();
                }
                p.executeBatch();
            }
            return totalBd.doubleValue();
        });
    }

    /**
     * Obtiene el historial de registros activos de una tabla de operaciones para mostrarlo en la vista
     * sin que la capa de presentación acceda directamente a la base de datos (RNF-05).
     *
     * @param tabla Nombre de la tabla de operación.
     * @return Par de (lista de nombres de columna, lista de filas).
     * @throws Exception Si la tabla no está permitida o falla la consulta SQL.
     */
    public Map<String, Object> listarHistorial(String tabla) throws Exception {
        if (!TABLAS_HISTORIAL.contains(tabla)) {
            throw new IllegalArgumentException("Tabla de historial no permitida: " + tabla);
        }
        var columns = new ArrayList<String>();
        var rows = new ArrayList<Object[]>();
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var st = c.createStatement();
             var rs = st.executeQuery("SELECT * FROM " + tabla + " WHERE activo = 1 ORDER BY 1 DESC LIMIT 200")) {
            int n = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= n; i++) {
                String colLabel = rs.getMetaData().getColumnLabel(i);
                if (!colLabel.equalsIgnoreCase("activo") && !colLabel.equalsIgnoreCase("fecha_borrado")) {
                    columns.add(colLabel);
                }
            }
            while (rs.next()) {
                Object[] row = new Object[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    row[i] = rs.getObject(columns.get(i));
                }
                rows.add(row);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("columnas", columns);
        res.put("filas", rows);
        return res;
    }

    /**
     * Devuelve la lista de opciones activas ({@code id}, {@code nombre}) para poblar combos de selección.
     * Para {@code obra} y {@code partida}, filtra además que la obra se encuentre activa (no Finalizada).
     *
     * @param tabla Nombre de la tabla maestra.
     * @return Lista de mapas con {@code id} y {@code nombre}.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> opciones(String tabla) throws Exception {
        String sql = switch (tabla) {
            case "obra" -> "SELECT id_obra, nombre FROM obra WHERE activo=1 AND UPPER(estado) NOT IN ('FINALIZADA','INACTIVA','CERRADA') ORDER BY nombre";
            case "obra_todas" -> "SELECT id_obra, nombre FROM obra WHERE activo=1 ORDER BY nombre";
            case "partida" -> "SELECT p.id_partida, CONCAT(o.nombre, ' — ', p.nombre) AS nombre FROM partida p JOIN obra o ON o.id_obra=p.id_obra WHERE p.activo=1 AND o.activo=1 AND UPPER(o.estado) NOT IN ('FINALIZADA','INACTIVA','CERRADA') ORDER BY o.nombre, p.nombre";
            case "material" -> "SELECT id_material, nombre FROM material WHERE activo=1 ORDER BY nombre";
            case "proveedor" -> "SELECT id_proveedor, razon_social FROM proveedor WHERE activo=1 ORDER BY razon_social";
            case "empleado" -> "SELECT id_empleado, nombres_apellidos FROM empleado WHERE activo=1 ORDER BY nombres_apellidos";
            default -> throw new IllegalArgumentException("Opción no permitida: " + tabla);
        };
        var out = new ArrayList<Map<String, Object>>();
        try (Connection x = ClsConexionBD.getInstancia().abrirConexion();
             var p = x.createStatement();
             ResultSet r = p.executeQuery(sql)) {
            while (r.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getInt(1));
                m.put("nombre", r.getString(2));
                out.add(m);
            }
        }
        return out;
    }

    private LocalDate[] validarObraActivaEnConexion(Connection c, int idObra) throws Exception {
        String sql = "SELECT estado, activo, fecha_inicio_plan, fecha_fin_plan FROM obra WHERE id_obra=? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idObra);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("activo") != 1) {
                    throw new IllegalArgumentException("La obra seleccionada no existe o se encuentra inactiva.");
                }
                String estado = rs.getString("estado");
                if (estado != null && (estado.equalsIgnoreCase("Finalizada") || estado.equalsIgnoreCase("Inactiva") || estado.equalsIgnoreCase("Cerrada"))) {
                    throw new IllegalArgumentException("La obra seleccionada no está activa (Estado: " + estado + ").");
                }
                return new LocalDate[]{
                    rs.getDate("fecha_inicio_plan").toLocalDate(),
                    rs.getDate("fecha_fin_plan").toLocalDate()
                };
            }
        }
    }

    @FunctionalInterface
    private interface Trabajo<T> {
        T ejecutar(Connection c) throws Exception;
    }

    private <T> T transaccion(Trabajo<T> trabajo) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion()) {
            c.setAutoCommit(false);
            try {
                T result = trabajo.ejecutar(c);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
