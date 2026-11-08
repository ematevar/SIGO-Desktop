package Negocio;

import config.ClsConstantes;
import Entidad.ClsUsuario;
import NegocioDAO.ClsUsuarioDAO;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para autenticación, control de intentos fallidos y gestión de usuarios (RF-01, RF-02).
 */
public class ClsUsuarioService {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final long BLOQUEO_MS = 30_000L;

    private final ClsUsuarioDAO dao;
    private final ClsCaptchaService captcha;

    private int intentosFallidos = 0;
    private long bloqueadoHastaMs = 0L;

    public ClsUsuarioService() {
        this(new ClsUsuarioDAO(), new ClsCaptchaService());
    }

    public ClsUsuarioService(ClsUsuarioDAO dao, ClsCaptchaService captcha) {
        this.dao = dao;
        this.captcha = captcha;
    }

    /**
     * Genera un nuevo código captcha alfanumérico de 6 caracteres.
     *
     * @return Código captcha generado.
     */
    public String nuevoCaptcha() {
        return captcha.generar();
    }

    /**
     * Lista todos los usuarios registrados (solo Administrador).
     *
     * @return Lista de usuarios.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> listar() throws Exception {
        return dao.listar();
    }

    /**
     * Lista todos los usuarios verificando el rol en sesión (RF-01, RF-02).
     *
     * @param rolSesion Rol del usuario autenticado.
     * @return Lista de usuarios.
     * @throws Exception Si no tiene permisos o falla la consulta.
     */
    public List<Map<String, Object>> listar(String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        return dao.listar();
    }

    /**
     * Lista los roles válidos del sistema.
     *
     * @return Lista de roles.
     * @throws Exception Si ocurre un error SQL.
     */
    public List<Map<String, Object>> roles() throws Exception {
        return dao.roles();
    }

    /**
     * Crea un nuevo usuario verificando unicidad de login, fortaleza de contraseña y rol válido (RF-02).
     *
     * @param nombre Nombre completo.
     * @param usuario Login de usuario.
     * @param clave Contraseña inicial.
     * @param rol ID del rol.
     * @throws Exception Si los datos son inválidos o el login ya existe.
     */
    public void crear(String nombre, String usuario, String clave, int rol) throws Exception {
        crear(nombre, usuario, clave, rol, null);
    }

    /**
     * Crea un nuevo usuario validando el rol de sesión (solo Administrador, RF-02).
     *
     * @param nombre Nombre completo.
     * @param usuario Login de usuario.
     * @param clave Contraseña inicial.
     * @param rol ID del rol.
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si los datos son inválidos o el rol no es Administrador.
     */
    public void crear(String nombre, String usuario, String clave, int rol, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        if (nombre == null || nombre.isBlank() || usuario == null || !usuario.trim().matches("[A-Za-z0-9._-]{3,40}")) {
            throw new IllegalArgumentException("Completa el nombre y un usuario válido (3 a 40 caracteres).");
        }
        if (clave == null || clave.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (rol <= 0) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }
        String nombreRol = dao.obtenerNombreRol(rol);
        if (nombreRol == null || "CONTADOR".equalsIgnoreCase(nombreRol)) {
            throw new IllegalArgumentException("El rol seleccionado no es válido según el perfil del sistema.");
        }
        if (dao.existeLogin(usuario.trim())) {
            throw new IllegalArgumentException("El nombre de usuario '" + usuario.trim() + "' ya está registrado.");
        }
        dao.crear(nombre.trim(), usuario.trim(), hash(clave), rol);
    }

    /**
     * Activa o desactiva un usuario impidiendo que quede el sistema sin Administradores activos (RF-02).
     *
     * @param usuario ID del usuario a modificar.
     * @param activo Nuevo estado activo.
     * @throws Exception Si se intenta desactivar al único Administrador activo.
     */
    public void cambiarEstado(int usuario, boolean activo) throws Exception {
        cambiarEstado(usuario, activo, null);
    }

    /**
     * Activa o desactiva un usuario validando el rol de sesión (RF-02).
     *
     * @param usuario ID del usuario a modificar.
     * @param activo Nuevo estado activo.
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si no tiene permisos o se desactiva al último Administrador.
     */
    public void cambiarEstado(int usuario, boolean activo, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        if (usuario <= 0) {
            throw new IllegalArgumentException("Usuario inválido.");
        }
        if (!activo) {
            Map<String, Object> info = dao.obtenerUsuarioRol(usuario);
            if (info == null) {
                throw new IllegalArgumentException("Usuario no encontrado.");
            }
            String rolActual = (String) info.get("nombre_rol");
            boolean estabaActivo = Boolean.TRUE.equals(info.get("activo"));
            if (estabaActivo && ClsConstantes.ROL_ADMIN.equals(ClsConstantes.normalizarRol(rolActual))) {
                if (dao.contarAdminsActivos() <= 1) {
                    throw new IllegalArgumentException("No se puede desactivar al único Administrador activo del sistema.");
                }
            }
        }
        dao.estado(usuario, activo);
    }

    /**
     * Actualiza el nombre y rol de un usuario cuidando que siempre exista al menos un Administrador activo (RF-02).
     *
     * @param usuario ID del usuario.
     * @param nombre Nuevo nombre completo.
     * @param rol Nuevo ID de rol.
     * @throws Exception Si los datos son inválidos o se quita el rol de Administrador al último admin.
     */
    public void actualizarDatos(int usuario, String nombre, int rol) throws Exception {
        actualizarDatos(usuario, nombre, rol, null);
    }

    /**
     * Actualiza el nombre y rol de un usuario validando permisos de Administrador (RF-02).
     *
     * @param usuario ID del usuario.
     * @param nombre Nuevo nombre completo.
     * @param rol Nuevo ID de rol.
     * @param rolSesion Rol del usuario en sesión.
     * @throws Exception Si ocurre un error de validación.
     */
    public void actualizarDatos(int usuario, String nombre, int rol, String rolSesion) throws Exception {
        ClsConstantes.verificarRol(rolSesion, ClsConstantes.ROL_ADMIN);
        if (usuario <= 0 || nombre == null || nombre.isBlank() || rol <= 0) {
            throw new IllegalArgumentException("Nombre o rol inválido.");
        }
        String nuevoNombreRol = dao.obtenerNombreRol(rol);
        if (nuevoNombreRol == null || "CONTADOR".equalsIgnoreCase(nuevoNombreRol)) {
            throw new IllegalArgumentException("El rol seleccionado no es válido.");
        }
        Map<String, Object> actual = dao.obtenerUsuarioRol(usuario);
        if (actual == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        String rolAnterior = (String) actual.get("nombre_rol");
        boolean activo = Boolean.TRUE.equals(actual.get("activo"));
        if (activo
                && ClsConstantes.ROL_ADMIN.equals(ClsConstantes.normalizarRol(rolAnterior))
                && !ClsConstantes.ROL_ADMIN.equals(ClsConstantes.normalizarRol(nuevoNombreRol))) {
            if (dao.contarAdminsActivos() <= 1) {
                throw new IllegalArgumentException("No se puede cambiar el rol del único Administrador activo.");
            }
        }
        dao.actualizarDatos(usuario, nombre.trim(), rol);
    }

    /**
     * Cambia la contraseña del usuario tras validar la contraseña actual.
     *
     * @param usuario ID del usuario.
     * @param actual Contraseña actual.
     * @param nueva Nueva contraseña (mínimo 8 caracteres).
     * @throws Exception Si la contraseña actual no coincide o la nueva es muy corta.
     */
    public void cambiarClave(int usuario, String actual, String nueva) throws Exception {
        if (nueva == null || nueva.length() < 8) {
            throw new IllegalArgumentException("La contraseña nueva debe tener al menos 8 caracteres.");
        }
        if (actual != null && actual.equals(nueva)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
        }
        if (!dao.autenticarPorId(usuario, actual)) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        dao.actualizarClave(usuario, hash(nueva));
    }

    /**
     * Genera un hash PBKDF2-HMAC-SHA256 con sal aleatoria.
     *
     * @param clave Contraseña en texto plano.
     * @return Cadena codificada con el hash.
     * @throws Exception Si falla el algoritmo criptográfico.
     */
    public String hash(String clave) throws Exception {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        var spec = new javax.crypto.spec.PBEKeySpec(clave.toCharArray(), salt, 120000, 256);
        byte[] result = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return "PBKDF2$120000$" + java.util.Base64.getEncoder().encodeToString(salt) + "$" + java.util.Base64.getEncoder().encodeToString(result);
    }

    /**
     * Inicia sesión validando usuario, contraseña, captcha y bloqueo temporal por intentos fallidos (RF-01).
     *
     * @param usuario Nombre de usuario.
     * @param clave Contraseña.
     * @param captchaEsperado Código captcha mostrado.
     * @param captchaIngresado Código captcha escrito por el usuario.
     * @return Usuario autenticado con su rol.
     * @throws Exception Si las credenciales o el captcha son inválidos, o si la cuenta está temporalmente bloqueada.
     */
    public ClsUsuario iniciarSesion(String usuario, String clave, String captchaEsperado, String captchaIngresado) throws Exception {
        long ahora = System.currentTimeMillis();
        if (ahora < bloqueadoHastaMs) {
            long segs = Math.max(1L, (bloqueadoHastaMs - ahora + 999) / 1000);
            throw new IllegalArgumentException("Demasiados intentos fallidos. Espera " + segs + " segundos antes de volver a intentar.");
        }
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            registrarFallo();
            throw new IllegalArgumentException("Ingresa usuario y contraseña.");
        }
        if (!captcha.validar(captchaEsperado, captchaIngresado)) {
            registrarFallo();
            throw new IllegalArgumentException("Captcha inválido.");
        }
        ClsUsuario resultado = dao.autenticar(usuario.trim(), clave);
        if (resultado == null) {
            registrarFallo();
            throw new IllegalArgumentException("Usuario o contraseña incorrectos, o cuenta inactiva.");
        }
        intentosFallidos = 0;
        bloqueadoHastaMs = 0L;
        return resultado;
    }

    private void registrarFallo() {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            bloqueadoHastaMs = System.currentTimeMillis() + BLOQUEO_MS;
            intentosFallidos = 0;
        }
    }
}
