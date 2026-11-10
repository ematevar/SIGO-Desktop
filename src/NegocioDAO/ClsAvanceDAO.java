package NegocioDAO;

import config.ClsConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Acceso a datos transaccional para el registro y cálculo de avances físicos por partida y obra (RF-05).
 */
public class ClsAvanceDAO {

    /**
     * Obtiene el último porcentaje de avance activo registrado para una partida.
     *
     * @param idPartida ID de la partida.
     * @return Último porcentaje registrado o {@code null} si aún no tiene avances.
     * @throws Exception Si ocurre un error SQL.
     */
    public Double ultimoPorcentaje(int idPartida) throws Exception {
        String sql = "SELECT porcentaje_avance FROM avance_partida "
                + "WHERE id_partida=? AND activo=1 ORDER BY fecha_reporte DESC, id_avance DESC LIMIT 1";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPartida);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : null;
            }
        }
    }

    /**
     * Registra un nuevo avance físico validando que la partida y su obra estén activas,
     * que el porcentaje no disminuya y que la fecha no sea regresiva, y devuelve el
     * avance acumulado ponderado de la obra (RF-05).
     *
     * @param idPartida ID de la partida.
     * @param fecha Fecha del reporte.
     * @param porcentaje Porcentaje acumulado de la partida (0 a 100).
     * @param observaciones Observaciones opcionales.
     * @param idUsuario ID del usuario que registra el avance.
     * @return Avance físico acumulado ponderado de la obra tras el registro (%).
     * @throws Exception Si la partida/obra no está activa o los datos violan las reglas de negocio.
     */
    public double registrar(int idPartida, LocalDate fecha, double porcentaje, String observaciones, int idUsuario) throws Exception {
        String sql = "INSERT INTO avance_partida(id_partida, fecha_reporte, porcentaje_avance, observaciones, id_usuario) VALUES(?,?,?,?,?)";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion()) {
            c.setAutoCommit(false);
            try {
                int idObra;
                String lockSql = "SELECT p.id_obra, p.activo AS p_activo, o.activo AS o_activo, o.estado "
                        + "FROM partida p JOIN obra o ON o.id_obra=p.id_obra WHERE p.id_partida=? FOR UPDATE";
                try (PreparedStatement lock = c.prepareStatement(lockSql)) {
                    lock.setInt(1, idPartida);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("La partida seleccionada no existe.");
                        }
                        if (rs.getInt("p_activo") != 1) {
                            throw new IllegalArgumentException("La partida seleccionada se encuentra inactiva.");
                        }
                        String estadoObra = rs.getString("estado");
                        if (rs.getInt("o_activo") != 1
                                || (estadoObra != null && (estadoObra.equalsIgnoreCase("Finalizada")
                                || estadoObra.equalsIgnoreCase("Inactiva")
                                || estadoObra.equalsIgnoreCase("Cerrada")))) {
                            throw new IllegalArgumentException("Solo se puede registrar avance físico en partidas de obras activas (RF-05).");
                        }
                        idObra = rs.getInt("id_obra");
                    }
                }
                String latestSql = "SELECT fecha_reporte, porcentaje_avance FROM avance_partida "
                        + "WHERE id_partida=? AND activo=1 ORDER BY fecha_reporte DESC, id_avance DESC LIMIT 1";
                try (PreparedStatement latest = c.prepareStatement(latestSql)) {
                    latest.setInt(1, idPartida);
                    try (ResultSet rs = latest.executeQuery()) {
                        if (rs.next()) {
                            if (fecha.isBefore(rs.getDate(1).toLocalDate())) {
                                throw new IllegalArgumentException("La fecha no puede ser anterior al último avance registrado (" + rs.getDate(1) + ").");
                            }
                            if (porcentaje < rs.getDouble(2)) {
                                throw new IllegalArgumentException("El avance acumulado no puede disminuir respecto al último registrado (" + rs.getDouble(2) + " %).");
                            }
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, idPartida);
                    ps.setDate(2, java.sql.Date.valueOf(fecha));
                    ps.setDouble(3, porcentaje);
                    ps.setString(4, observaciones);
                    ps.setInt(5, idUsuario);
                    ps.executeUpdate();
                }
                double avanceObra = calcularAvanceObraEnConexion(c, idObra);
                c.commit();
                return avanceObra;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Calcula el avance físico acumulado ponderado de una obra a partir del último avance activo
     * de cada una de sus partidas activas (RF-05).
     *
     * @param idObra ID de la obra.
     * @return Porcentaje de avance acumulado de la obra (0 a 100).
     * @throws Exception Si ocurre un error SQL.
     */
    public double avanceAcumuladoObra(int idObra) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion()) {
            return calcularAvanceObraEnConexion(c, idObra);
        }
    }

    private double calcularAvanceObraEnConexion(Connection c, int idObra) throws Exception {
        String sql = "SELECT COALESCE(ROUND(SUM(p.peso_presupuestal * COALESCE(("
                + "SELECT a.porcentaje_avance FROM avance_partida a "
                + "WHERE a.id_partida=p.id_partida AND a.activo=1 "
                + "ORDER BY a.fecha_reporte DESC, a.id_avance DESC LIMIT 1"
                + "), 0)) / 100.0, 2), 0) "
                + "FROM partida p WHERE p.id_obra=? AND p.activo=1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idObra);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }
}
