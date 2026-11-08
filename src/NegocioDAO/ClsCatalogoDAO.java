package NegocioDAO;

import config.ClsConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRUD parametrizado y consultas de validación para las tablas maestras (RF-03, RF-04, RF-06, RF-09, RF-10).
 */
public class ClsCatalogoDAO {

    private static final Set<String> TABLAS = Set.of("obra", "partida", "material", "proveedor", "empleado", "rol");

    private String tabla(String nombre) {
        if (!TABLAS.contains(nombre)) {
            throw new IllegalArgumentException("Tabla de catálogo no permitida.");
        }
        return nombre;
    }

    /**
     * Obtiene los nombres de las columnas editables/visibles de una tabla maestra omitiendo campos de auditoría.
     *
     * @param nombre Nombre de la tabla.
     * @return Lista de nombres de columna.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<String> columnas(String nombre) throws Exception {
        String t = tabla(nombre);
        List<String> out = new ArrayList<>();
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var ps = c.prepareStatement("SELECT * FROM " + t + " LIMIT 0");
             ResultSet rs = ps.executeQuery()) {
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String col = rs.getMetaData().getColumnLabel(i);
                if (!col.equalsIgnoreCase("activo") && !col.equalsIgnoreCase("fecha_borrado")) {
                    out.add(col);
                }
            }
        }
        return out;
    }

    /**
     * Obtiene el nombre de la columna clave primaria de la tabla indicada.
     *
     * @param nombre Nombre de la tabla.
     * @return Nombre de la clave primaria.
     * @throws Exception Si ocurre un error SQL.
     */
    public String clavePrimaria(String nombre) throws Exception {
        String t = tabla(nombre);
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion()) {
            try (ResultSet rs = c.getMetaData().getPrimaryKeys(c.getCatalog(), null, t)) {
                if (rs.next()) {
                    return rs.getString("COLUMN_NAME");
                }
            }
        }
        throw new IllegalStateException("La tabla no tiene clave primaria: " + t);
    }

    /**
     * Lista los registros activos de la tabla indicada.
     *
     * @param nombre Nombre de la tabla.
     * @return Lista de mapas columna-valor.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> listar(String nombre) throws Exception {
        String t = tabla(nombre);
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + t + " WHERE activo=1 ORDER BY 1 DESC LIMIT 500")) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= n; i++) {
                    String col = rs.getMetaData().getColumnLabel(i);
                    if (!col.equalsIgnoreCase("activo") && !col.equalsIgnoreCase("fecha_borrado")) {
                        row.put(col, rs.getObject(i));
                    }
                }
                out.add(row);
            }
        }
        return out;
    }

    /**
     * Verifica si una obra existe, tiene {@code activo=1} y no está Finalizada ni Pausada/Inactiva (RF-04, RF-05, RF-08, RF-13).
     *
     * @param idObra ID de la obra.
     * @return {@code true} si la obra está activa para registrar partidas y operaciones.
     * @throws Exception Si ocurre un error SQL.
     */
    public boolean esObraActiva(int idObra) throws Exception {
        String sql = "SELECT estado, activo FROM obra WHERE id_obra=?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idObra);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("activo") != 1) {
                    return false;
                }
                String estado = rs.getString("estado");
                return estado == null || (!estado.equalsIgnoreCase("Finalizada") && !estado.equalsIgnoreCase("Inactiva") && !estado.equalsIgnoreCase("Cerrada"));
            }
        }
    }

    /**
     * Calcula la suma actual de {@code peso_presupuestal} de las partidas activas de una obra,
     * excluyendo opcionalmente una partida que se está editando (RF-04).
     *
     * @param idObra ID de la obra.
     * @param idPartidaExcluir ID de partida a excluir (o {@code null} al crear).
     * @return Suma de pesos presupuestales actuales.
     * @throws Exception Si ocurre un error SQL.
     */
    public double sumaPesosPartidas(int idObra, Object idPartidaExcluir) throws Exception {
        String sql = idPartidaExcluir == null
                ? "SELECT COALESCE(SUM(peso_presupuestal), 0) FROM partida WHERE id_obra=? AND activo=1"
                : "SELECT COALESCE(SUM(peso_presupuestal), 0) FROM partida WHERE id_obra=? AND activo=1 AND id_partida<>?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idObra);
            if (idPartidaExcluir != null) {
                ps.setInt(2, Integer.parseInt(idPartidaExcluir.toString()));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /**
     * Verifica si una obra tiene partidas o movimientos registrados que impidan su eliminación (RF-03).
     *
     * @param idObra ID de la obra.
     * @return {@code true} si tiene partidas, asistencias, ingresos, egresos o cierres asociados.
     * @throws Exception Si ocurre un error SQL.
     */
    public boolean obraTieneMovimientos(int idObra) throws Exception {
        String[] consultas = {
            "SELECT 1 FROM partida WHERE id_obra=? AND activo=1 LIMIT 1",
            "SELECT 1 FROM asistencia WHERE id_obra=? AND activo=1 LIMIT 1",
            "SELECT 1 FROM ingreso WHERE id_obra=? AND activo=1 LIMIT 1",
            "SELECT 1 FROM egreso WHERE id_obra=? AND activo=1 LIMIT 1",
            "SELECT 1 FROM cierre_planilla WHERE id_obra=? AND activo=1 LIMIT 1"
        };
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion()) {
            for (String q : consultas) {
                try (PreparedStatement ps = c.prepareStatement(q)) {
                    ps.setInt(1, idObra);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Cierra o finaliza una obra cuando tiene movimientos y no puede eliminarse (RF-03).
     *
     * @param idObra ID de la obra.
     * @throws Exception Si ocurre un error SQL.
     */
    public void cerrarObra(int idObra) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement("UPDATE obra SET estado='Finalizada' WHERE id_obra=? AND activo=1")) {
            ps.setInt(1, idObra);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("No se encontró la obra.");
            }
        }
    }

    /**
     * Obtiene el stock actual de un material para validar antes de darle de baja (RF-06).
     *
     * @param idMaterial ID del material.
     * @return Stock disponible.
     * @throws Exception Si ocurre un error SQL.
     */
    public double stockActualMaterial(int idMaterial) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement("SELECT stock_actual FROM material WHERE id_material=?")) {
            ps.setInt(1, idMaterial);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("El material no existe.");
                }
                return rs.getDouble(1);
            }
        }
    }

    /**
     * Verifica si un proveedor tiene compras registradas (RF-09).
     *
     * @param idProveedor ID del proveedor.
     * @return {@code true} si tiene compras asociadas.
     * @throws Exception Si ocurre un error SQL.
     */
    public boolean proveedorTieneCompras(int idProveedor) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM compra_material WHERE id_proveedor=? LIMIT 1")) {
            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Verifica si ya existe un proveedor con el mismo RUC excluyendo opcionalmente el ID actual (RF-09).
     *
     * @param ruc RUC de 11 dígitos.
     * @param idExcluir ID del proveedor actual al editar, o {@code null}.
     * @return {@code true} si el RUC está duplicado.
     * @throws Exception Si ocurre un error SQL.
     */
    public boolean existeRuc(String ruc, Object idExcluir) throws Exception {
        String sql = idExcluir == null
                ? "SELECT 1 FROM proveedor WHERE ruc=?"
                : "SELECT 1 FROM proveedor WHERE ruc=? AND id_proveedor<>?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ruc);
            if (idExcluir != null) {
                ps.setInt(2, Integer.parseInt(idExcluir.toString()));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Verifica si ya existe un trabajador con el mismo DNI excluyendo opcionalmente el ID actual (RF-10).
     *
     * @param dni DNI de 8 dígitos.
     * @param idExcluir ID del empleado actual al editar, o {@code null}.
     * @return {@code true} si el DNI está duplicado.
     * @throws Exception Si ocurre un error SQL.
     */
    public boolean existeDni(String dni, Object idExcluir) throws Exception {
        String sql = idExcluir == null
                ? "SELECT 1 FROM empleado WHERE dni=?"
                : "SELECT 1 FROM empleado WHERE dni=? AND id_empleado<>?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dni);
            if (idExcluir != null) {
                ps.setInt(2, Integer.parseInt(idExcluir.toString()));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Verifica si una partida tiene avances, consumos o producción registrados.
     *
     * @param idPartida ID de la partida.
     * @return {@code true} si tiene movimientos asociados.
     * @throws Exception Si ocurre un error SQL.
     */
    public boolean partidaTieneMovimientos(int idPartida) throws Exception {
        String[] consultas = {
            "SELECT 1 FROM avance_partida WHERE id_partida=? AND activo=1 LIMIT 1",
            "SELECT 1 FROM consumo_material WHERE id_partida=? AND activo=1 LIMIT 1",
            "SELECT 1 FROM produccion_empleado WHERE id_partida=? AND activo=1 LIMIT 1"
        };
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion()) {
            for (String q : consultas) {
                try (PreparedStatement ps = c.prepareStatement(q)) {
                    ps.setInt(1, idPartida);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Inserta o actualiza un registro de catálogo asegurando que {@code stock_actual} sea de solo lectura (RF-06).
     *
     * @param nombre Nombre de la tabla.
     * @param pk Nombre de la clave primaria.
     * @param id Valor del ID ({@code null} para insertar).
     * @param values Mapa de valores por columna.
     * @throws Exception Si ocurre un error SQL.
     */
    public void guardar(String nombre, String pk, Object id, Map<String, Object> values) throws Exception {
        String t = tabla(nombre);
        List<String> cols = columnas(t);
        List<String> editables = cols.stream()
                .filter(c -> !c.equalsIgnoreCase(pk) && !(t.equals("material") && c.equals("stock_actual")))
                .toList();
        if (editables.isEmpty()) {
            throw new IllegalArgumentException("No hay campos para guardar.");
        }
        String sql;
        if (id == null) {
            String fields = String.join(",", editables);
            String marks = String.join(",", java.util.Collections.nCopies(editables.size(), "?"));
            sql = "INSERT INTO " + t + " (" + fields + ") VALUES (" + marks + ")";
        } else {
            String assigns = String.join(",", editables.stream().map(c -> c + "=?").toList());
            sql = "UPDATE " + t + " SET " + assigns + " WHERE " + pk + "=?";
        }
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            for (String col : editables) {
                ps.setObject(i++, values.get(col));
            }
            if (id != null) {
                ps.setObject(i, id);
            }
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("No se guardó ningún registro.");
            }
        }
    }

    /**
     * Realiza el borrado lógico (desactivación) de un registro maestro estableciendo {@code activo=0}
     * y {@code fecha_borrado=CURRENT_TIMESTAMP}.
     *
     * @param nombre Nombre de la tabla.
     * @param pk Clave primaria.
     * @param id ID del registro.
     * @throws Exception Si el registro no existe o ocurre un error SQL.
     */
    public void eliminar(String nombre, String pk, Object id) throws Exception {
        String t = tabla(nombre);
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement("UPDATE " + t + " SET activo=0, fecha_borrado=CURRENT_TIMESTAMP WHERE " + pk + "=?")) {
            ps.setObject(1, id);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("No se encontró el registro.");
            }
        }
    }
}
