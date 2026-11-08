package NegocioDAO;

import config.ClsConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos transaccional para consumos de materiales y alertas de stock mínimo (RF-06, RF-08).
 */
public class ClsMaterialDAO {

    /**
     * Registra un consumo de material verificando que la partida y su obra estén activas,
     * que el material esté activo y tenga stock suficiente, y descuenta el stock en la misma transacción (RF-08).
     *
     * @param material ID del material.
     * @param partida ID de la partida.
     * @param usuario ID del usuario responsable.
     * @param fecha Fecha del consumo.
     * @param cantidad Cantidad a consumir (> 0).
     * @throws Exception Si la obra/partida está inactiva o el stock es insuficiente.
     */
    public void registrarConsumo(int material, int partida, int usuario, LocalDate fecha, double cantidad) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion()) {
            c.setAutoCommit(false);
            try {
                String sqlPartida = "SELECT p.activo AS p_activo, o.activo AS o_activo, o.estado "
                        + "FROM partida p JOIN obra o ON o.id_obra=p.id_obra WHERE p.id_partida=? FOR UPDATE";
                try (PreparedStatement ps = c.prepareStatement(sqlPartida)) {
                    ps.setInt(1, partida);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("La partida seleccionada no existe.");
                        }
                        if (rs.getInt("p_activo") != 1) {
                            throw new IllegalArgumentException("La partida seleccionada está inactiva.");
                        }
                        String estadoObra = rs.getString("estado");
                        if (rs.getInt("o_activo") != 1
                                || (estadoObra != null && (estadoObra.equalsIgnoreCase("Finalizada")
                                || estadoObra.equalsIgnoreCase("Inactiva")
                                || estadoObra.equalsIgnoreCase("Cerrada")))) {
                            throw new IllegalArgumentException("Solo se puede registrar consumo de materiales en partidas de una obra activa (RF-08).");
                        }
                    }
                }
                double stock;
                try (PreparedStatement ps = c.prepareStatement("SELECT stock_actual, activo FROM material WHERE id_material=? FOR UPDATE")) {
                    ps.setInt(1, material);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt("activo") != 1) {
                            throw new IllegalArgumentException("El material seleccionado no existe o está inactivo.");
                        }
                        stock = rs.getDouble("stock_actual");
                    }
                }
                if (stock < cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente. Disponible: " + stock);
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO consumo_material(id_partida, id_material, fecha_consumo, cantidad, id_usuario) VALUES(?,?,?,?,?)")) {
                    ps.setInt(1, partida);
                    ps.setInt(2, material);
                    ps.setDate(3, java.sql.Date.valueOf(fecha));
                    ps.setDouble(4, cantidad);
                    ps.setInt(5, usuario);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE material SET stock_actual=stock_actual-? WHERE id_material=?")) {
                    ps.setDouble(1, cantidad);
                    ps.setInt(2, material);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * Devuelve la diferencia {@code stock_actual - stock_minimo} para determinar si el material
     * quedó igual o por debajo del stock mínimo ({@code <= 0}, RF-08).
     *
     * @param material ID del material.
     * @return Diferencia entre stock actual y stock mínimo.
     * @throws Exception Si el material no existe o ocurre un error SQL.
     */
    public double stockMinimo(int material) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement("SELECT stock_actual, stock_minimo FROM material WHERE id_material=?")) {
            ps.setInt(1, material);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("El material seleccionado no existe.");
                }
                return rs.getDouble(1) - rs.getDouble(2);
            }
        }
    }

    /**
     * Calcula el consumo acumulado de un material específico dentro de una partida (RF-08).
     *
     * @param partida ID de la partida.
     * @param material ID del material.
     * @return Cantidad total consumida en esa partida.
     * @throws Exception Si ocurre un error SQL.
     */
    public double consumoAcumuladoPartida(int partida, int material) throws Exception {
        String sql = "SELECT COALESCE(SUM(cantidad), 0) FROM consumo_material WHERE id_partida=? AND id_material=? AND activo=1";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, partida);
            ps.setInt(2, material);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /**
     * Lista los materiales activos cuyo {@code stock_actual <= stock_minimo} para mostrar alertas al Almacenero (RF-06, RF-08).
     *
     * @return Lista de materiales en alerta de stock mínimo.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> listarAlertasStockMinimo() throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT id_material, nombre, unidad_medida, stock_actual, stock_minimo "
                + "FROM material WHERE activo=1 AND stock_actual <= stock_minimo ORDER BY (stock_actual - stock_minimo) ASC, nombre ASC";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id_material", rs.getInt(1));
                m.put("nombre", rs.getString(2));
                m.put("unidad_medida", rs.getString(3));
                m.put("stock_actual", rs.getDouble(4));
                m.put("stock_minimo", rs.getDouble(5));
                out.add(m);
            }
        }
        return out;
    }
}
