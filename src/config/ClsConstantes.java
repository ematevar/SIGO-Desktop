package config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Constantes globales de la aplicación, roles, permisos y utilidades de seguridad.
 */
public final class ClsConstantes {

    public static final String NOMBRE_APP = "SIGOC — Gestión de Obras Civiles";

    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_RESIDENTE = "RESIDENTE";
    public static final String ROL_ALMACENERO = "ALMACENERO";

    /**
     * Módulos disponibles en el panel principal.
     */
    public static final String[] MODULOS = {
        "OBRAS", "PARTIDAS", "AVANCES", "MATERIALES", "PROVEEDORES",
        "COMPRAS", "CONSUMOS", "PERSONAL", "ASISTENCIA", "PRODUCCION",
        "FINANZAS", "REPORTES", "USUARIOS"
    };

    private ClsConstantes() {
    }

    /**
     * Normaliza el nombre de rol para mantener compatibilidad con datos previos
     * y los tres perfiles definidos en el SRS (Administrador, Residente de Obra, Almacenero).
     *
     * @param rol Nombre del rol en sesión o base de datos.
     * @return Clave canónica del rol: ADMIN, RESIDENTE o ALMACENERO.
     */
    public static String normalizarRol(String rol) {
        if (rol == null) {
            return "";
        }
        String r = rol.trim().toUpperCase();
        if (r.contains("ADMIN")) {
            return ROL_ADMIN;
        }
        if (r.contains("RESIDENTE") || r.contains("INGENIERO")) {
            return ROL_RESIDENTE;
        }
        if (r.contains("ALMACEN")) {
            return ROL_ALMACENERO;
        }
        return r;
    }

    /**
     * Devuelve el nombre legible del rol según el SRS.
     *
     * @param rol Rol en sesión.
     * @return Etiqueta descriptiva del perfil.
     */
    public static String etiquetaRol(String rol) {
        return switch (normalizarRol(rol)) {
            case ROL_ADMIN -> "Administrador";
            case ROL_RESIDENTE -> "Residente de Obra";
            case ROL_ALMACENERO -> "Almacenero";
            default -> rol == null ? "" : rol;
        };
    }

    /**
     * Verifica si el rol proporcionado tiene permiso para ejecutar una operación.
     * Si {@code rolSesion} es {@code null} (por ejemplo, llamadas internas de prueba sin sesión),
     * no bloquea a menos que se especifique un rol inválido explícito.
     *
     * @param rolSesion Rol del usuario autenticado.
     * @param rolesPermitidos Conjunto de roles canónicos permitidos.
     */
    public static void verificarRol(String rolSesion, String... rolesPermitidos) {
        if (rolSesion == null) {
            return;
        }
        String norm = normalizarRol(rolSesion);
        for (String permitido : rolesPermitidos) {
            if (norm.equals(normalizarRol(permitido))) {
                return;
            }
        }
        throw new SecurityException("Acceso denegado: su perfil (" + etiquetaRol(rolSesion) + ") no tiene permisos para realizar esta operación.");
    }

    /**
     * Traduce excepciones técnicas (SQL, red, formato) a mensajes claros para el usuario
     * y registra el detalle técnico en {@code sigoc_error.log} (RNF-08).
     *
     * @param ex Excepción capturada.
     * @return Mensaje comprensible para el usuario final.
     */
    public static String mensajeUsuario(Throwable ex) {
        if (ex == null) {
            return "Ocurrió un error inesperado.";
        }
        if (ex instanceof IllegalArgumentException || ex instanceof SecurityException || ex instanceof IllegalStateException) {
            String msg = ex.getMessage();
            if (msg != null && !msg.isBlank() && !contieneDetalleTecnico(msg)) {
                return msg;
            }
        }
        registrarLog(ex);
        String raw = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (raw.contains("duplicate entry") || raw.contains("unique")) {
            if (raw.contains("ruc")) {
                return "Ya existe un proveedor registrado con ese número de RUC.";
            }
            if (raw.contains("dni")) {
                return "Ya existe un trabajador registrado con ese número de DNI.";
            }
            if (raw.contains("usuario_login") || raw.contains("usuario")) {
                return "El nombre de usuario ya está en uso. Elige otro.";
            }
            if (raw.contains("comprobante")) {
                return "Ya se registró ese número de comprobante para el proveedor seleccionado.";
            }
            return "El registro ya existe (valor duplicado). Verifica los datos ingresados.";
        }
        if (raw.contains("foreign key") || raw.contains("constraint fails")) {
            return "No se puede completar la operación porque el registro está relacionado con otros movimientos del sistema.";
        }
        if (raw.contains("data truncation") || raw.contains("out of range") || raw.contains("incorrect")) {
            return "Uno de los valores ingresados excede el límite permitido o tiene un formato inválido.";
        }
        if (raw.contains("communications link failure") || raw.contains("connection") || raw.contains("timeout") || raw.contains("access denied")) {
            return "No se pudo establecer conexión con la base de datos. Verifique su red y el archivo config.properties.";
        }
        if (ex instanceof IllegalArgumentException || ex instanceof SecurityException) {
            return ex.getMessage();
        }
        return "No se pudo completar la operación solicitada. Se registró el detalle técnico en el log del sistema.";
    }

    private static boolean contieneDetalleTecnico(String msg) {
        String m = msg.toLowerCase();
        return m.contains("jdbc:") || m.contains("sqlexception") || m.contains("communications link") || m.contains("at java.");
    }

    /**
     * Registra un error técnico en el archivo local {@code sigoc_error.log}.
     *
     * @param ex Excepción a registrar.
     */
    public static void registrarLog(Throwable ex) {
        try {
            String linea = "[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] "
                    + ex.getClass().getName() + ": " + ex.getMessage() + System.lineSeparator();
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("sigoc_error.log"),
                    linea,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
    }
}
