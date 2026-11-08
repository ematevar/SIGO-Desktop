package NegocioDAO;

import config.ClsConexionBD;
import Entidad.ClsUsuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos para autenticación, usuarios y roles (RF-01, RF-02).
 */
public class ClsUsuarioDAO {

    /**
     * Verifica la contraseña actual de un usuario activo por su identificador.
     *
     * @param usuario ID del usuario.
     * @param clave Contraseña en texto plano a comprobar.
     * @return {@code true} si la contraseña coincide y el usuario está activo.
     * @throws Exception Si ocurre un error de acceso a datos.
     */
    public boolean autenticarPorId(int usuario, String clave) throws Exception {
        String sql = "SELECT contrasena_hash FROM usuario WHERE id_usuario=? AND activo=1";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, usuario);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() && verificar(clave, rs.getString(1));
            }
        }
    }

    /**
     * Lista todos los usuarios registrados en el sistema indicando su estado de actividad (RF-02).
     *
     * @return Lista de mapas con los datos de cada usuario.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> listar() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.usuario_login, r.nombre_rol, u.activo "
                + "FROM usuario u JOIN rol r ON r.id_rol=u.id_rol ORDER BY u.id_usuario";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                var row = new LinkedHashMap<String, Object>();
                row.put("id_usuario", rs.getInt(1));
                row.put("nombre_completo", rs.getString(2));
                row.put("usuario_login", rs.getString(3));
                row.put("nombre_rol", rs.getString(4));
                row.put("estado", rs.getInt(5));
                row.put("activo", rs.getInt(5));
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Lista los roles activos válidos según los perfiles del SRS (Administrador, Residente de Obra, Almacenero).
     *
     * @return Lista de roles disponibles.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> roles() throws Exception {
        var rows = new ArrayList<Map<String, Object>>();
        String sql = "SELECT id_rol, nombre_rol FROM rol WHERE activo=1 AND UPPER(nombre_rol) NOT IN ('CONTADOR') ORDER BY id_rol";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                var row = new LinkedHashMap<String, Object>();
                row.put("id", rs.getInt(1));
                row.put("nombre", rs.getString(2));
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Comprueba si ya existe un usuario con el login indicado.
     *
     * @param login Nombre de usuario.
     * @return {@code true} si ya está registrado.
     * @throws Exception Si ocurre un error SQL.
     */
    public boolean existeLogin(String login) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement p = c.prepareStatement("SELECT 1 FROM usuario WHERE LOWER(usuario_login)=LOWER(?)")) {
            p.setString(1, login);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Cuenta cuántos administradores activos existen actualmente en la base de datos.
     *
     * @return Cantidad de administradores activos.
     * @throws Exception Si ocurre un error SQL.
     */
    public int contarAdminsActivos() throws Exception {
        String sql = "SELECT COUNT(*) FROM usuario u JOIN rol r ON r.id_rol=u.id_rol "
                + "WHERE u.activo=1 AND UPPER(r.nombre_rol) LIKE '%ADMIN%'";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             var s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Obtiene el nombre del rol actual y estado activo de un usuario específico.
     *
     * @param idUsuario ID del usuario.
     * @return Mapa con {@code nombre_rol} y {@code activo}, o {@code null} si no existe.
     * @throws Exception Si ocurre un error SQL.
     */
    public Map<String, Object> obtenerUsuarioRol(int idUsuario) throws Exception {
        String sql = "SELECT r.nombre_rol, u.activo FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.id_usuario=?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nombre_rol", rs.getString(1));
                m.put("activo", rs.getInt(2) == 1);
                return m;
            }
        }
    }

    /**
     * Obtiene el nombre de un rol por su ID.
     *
     * @param idRol ID del rol.
     * @return Nombre del rol o {@code null}.
     * @throws Exception Si ocurre un error SQL.
     */
    public String obtenerNombreRol(int idRol) throws Exception {
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement("SELECT nombre_rol FROM rol WHERE id_rol=? AND activo=1")) {
            ps.setInt(1, idRol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Crea un nuevo usuario activo en el sistema.
     *
     * @param nombre Nombre completo.
     * @param login Nombre de usuario único.
     * @param hash Hash PBKDF2 de la contraseña.
     * @param rol ID del rol asignado.
     * @throws Exception Si ocurre un error SQL.
     */
    public void crear(String nombre, String login, String hash, int rol) throws Exception {
        String sql = "INSERT INTO usuario(nombre_completo, usuario_login, contrasena_hash, id_rol, estado, activo) VALUES(?,?,?,?,1,1)";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, nombre);
            p.setString(2, login);
            p.setString(3, hash);
            p.setInt(4, rol);
            p.executeUpdate();
        }
    }

    /**
     * Activa o desactiva lógicamente a un usuario (RF-02), sincronizando {@code activo} y {@code estado}.
     *
     * @param usuario ID del usuario.
     * @param activo {@code true} para activar, {@code false} para desactivar.
     * @throws Exception Si el usuario no existe o falla la consulta.
     */
    public void estado(int usuario, boolean activo) throws Exception {
        String sql = "UPDATE usuario SET activo=?, estado=?, fecha_borrado=CASE WHEN ?=0 THEN CURRENT_TIMESTAMP ELSE NULL END WHERE id_usuario=?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement p = c.prepareStatement(sql)) {
            int val = activo ? 1 : 0;
            p.setInt(1, val);
            p.setInt(2, val);
            p.setInt(3, val);
            p.setInt(4, usuario);
            if (p.executeUpdate() != 1) {
                throw new IllegalArgumentException("Usuario no encontrado.");
            }
        }
    }

    /**
     * Actualiza el nombre completo y el rol de un usuario existente.
     *
     * @param usuario ID del usuario.
     * @param nombre Nuevo nombre completo.
     * @param rol Nuevo ID de rol.
     * @throws Exception Si ocurre un error SQL.
     */
    public void actualizarDatos(int usuario, String nombre, int rol) throws Exception {
        String sql = "UPDATE usuario SET nombre_completo=?, id_rol=? WHERE id_usuario=?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, nombre);
            p.setInt(2, rol);
            p.setInt(3, usuario);
            if (p.executeUpdate() != 1) {
                throw new IllegalArgumentException("Usuario no encontrado.");
            }
        }
    }

    /**
     * Actualiza el hash de contraseña de un usuario.
     *
     * @param usuario ID del usuario.
     * @param hash Nuevo hash PBKDF2.
     * @throws Exception Si ocurre un error SQL.
     */
    public void actualizarClave(int usuario, String hash) throws Exception {
        String sql = "UPDATE usuario SET contrasena_hash=? WHERE id_usuario=?";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, hash);
            p.setInt(2, usuario);
            if (p.executeUpdate() != 1) {
                throw new IllegalArgumentException("Usuario no encontrado.");
            }
        }
    }

    /**
     * Autentica a un usuario por su login y contraseña verificando que se encuentre activo (RF-01, RF-02).
     *
     * @param usuario Nombre de usuario.
     * @param clave Contraseña ingresada.
     * @return Instancia de {@link ClsUsuario} si las credenciales son válidas, o {@code null}.
     * @throws Exception Si ocurre un error SQL.
     */
    public ClsUsuario autenticar(String usuario, String clave) throws Exception {
        String sql = "SELECT u.id_usuario, u.usuario_login, u.contrasena_hash, r.nombre_rol "
                + "FROM usuario u JOIN rol r ON r.id_rol=u.id_rol "
                + "WHERE u.usuario_login=? AND u.activo=1 AND u.estado=1 AND r.activo=1";
        try (Connection c = ClsConexionBD.getInstancia().abrirConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String hashAlmacenado = rs.getString("contrasena_hash");
                if (!verificar(clave, hashAlmacenado)) {
                    return null;
                }
                boolean esClaveInicial = hashAlmacenado.contains("U0lHT0MtREVNT1NUUkFDSU9OLVNBTFQ=");
                return new ClsUsuario(
                        rs.getInt("id_usuario"),
                        rs.getString("usuario_login"),
                        rs.getString("nombre_rol"),
                        esClaveInicial
                );
            }
        }
    }

    private boolean verificar(String clave, String almacenada) throws Exception {
        if (almacenada == null || !almacenada.startsWith("PBKDF2$")) {
            return false;
        }
        String[] p = almacenada.split("\\$", -1);
        if (p.length != 4) {
            return false;
        }
        int rondas = Integer.parseInt(p[1]);
        if (rondas < 10000 || rondas > 1000000) {
            return false;
        }
        var spec = new javax.crypto.spec.PBEKeySpec(
                clave.toCharArray(),
                java.util.Base64.getDecoder().decode(p[2]),
                rondas,
                256
        );
        byte[] esperado = java.util.Base64.getDecoder().decode(p[3]);
        byte[] actual = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return java.security.MessageDigest.isEqual(esperado, actual);
    }
}
