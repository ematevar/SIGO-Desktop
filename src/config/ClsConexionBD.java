package config;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Centraliza la configuración y un pool ligero de conexiones JDBC hacia MySQL 8 (RNF-04, RNF-06).
 * Evita abrir un handshake TCP/TLS nuevo en cada operación contra el servidor en la nube,
 * garantizando tiempos de respuesta de consultas inferiores a 2 segundos (RNF-06).
 */
public final class ClsConexionBD {

    private static final ClsConexionBD INSTANCIA = new ClsConexionBD();
    private static final int MAX_POOL_SIZE = 4;

    private final Properties propiedades = new Properties();
    private final BlockingQueue<Connection> pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

    private ClsConexionBD() {
        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("config.properties"))) {
            propiedades.load(in);
        } catch (java.nio.file.NoSuchFileException ignored) {
        } catch (Exception ex) {
            ClsConstantes.registrarLog(ex);
            throw new IllegalStateException("No se pudo leer config.properties", ex);
        }
        try (InputStream in = ClsConexionBD.class.getClassLoader().getResourceAsStream("config.properties.example")) {
            if (in != null) {
                Properties ejemplo = new Properties();
                ejemplo.load(in);
                for (String key : ejemplo.stringPropertyNames()) {
                    propiedades.putIfAbsent(key, ejemplo.getProperty(key));
                }
            }
        } catch (Exception ex) {
            ClsConstantes.registrarLog(ex);
            throw new IllegalStateException("No se pudo leer configuración de ejemplo", ex);
        }
    }

    /**
     * Obtiene la instancia única del gestor de conexiones.
     *
     * @return Instancia de {@code ClsConexionBD}.
     */
    public static ClsConexionBD getInstancia() {
        return INSTANCIA;
    }

    /**
     * Obtiene una conexión JDBC activa reutilizando conexiones del pool si están disponibles,
     * o abriendo una nueva si el pool está vacío (RNF-04, RNF-06). Al invocar {@code close()}
     * sobre la conexión devuelta, esta se restaura a {@code autoCommit=true} y regresa al pool.
     *
     * @return Conexión activa a MySQL.
     * @throws SQLException Si ocurre un error de conexión.
     */
    public Connection abrirConexion() throws SQLException {
        Connection raw = pool.poll();
        while (raw != null) {
            try {
                if (!raw.isClosed()) {
                    raw.setAutoCommit(true);
                    return envolverEnPool(raw);
                }
            } catch (SQLException ignored) {
            }
            cerrarSilenciosamente(raw);
            raw = pool.poll();
        }
        return envolverEnPool(crearConexionFisica());
    }

    private Connection crearConexionFisica() throws SQLException {
        String url = propiedades.getProperty("db.url");
        if (url == null || url.isBlank()) {
            String host = requerido("db.host");
            String db = requerido("db.name");
            boolean usarSsl = Boolean.parseBoolean(propiedades.getProperty("db.ssl", "true"));
            String sslMode = usarSsl ? "PREFERRED" : "DISABLED";
            String zona = propiedades.getProperty("db.timezone", "America/Lima");
            url = "jdbc:mysql://" + host + ":" + propiedades.getProperty("db.port", "3306") + "/" + db
                    + "?sslMode=" + sslMode
                    + "&serverTimezone=" + zona
                    + "&connectTimeout=5000&socketTimeout=10000"
                    + "&useServerPrepStmts=true&cachePrepStmts=true";
        }
        String user = requerido("db.user");
        String password = requerido("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private Connection envolverEnPool(Connection fisica) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean cerrada = false;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("close".equals(name)) {
                    if (!cerrada) {
                        cerrada = true;
                        devolverAlPool(fisica);
                    }
                    return null;
                }
                if ("isClosed".equals(name)) {
                    return cerrada || fisica.isClosed();
                }
                try {
                    return method.invoke(fisica, args);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            }
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private void devolverAlPool(Connection fisica) {
        try {
            if (!fisica.isClosed()) {
                if (!fisica.getAutoCommit()) {
                    fisica.rollback();
                    fisica.setAutoCommit(true);
                }
                if (!pool.offer(fisica)) {
                    fisica.close();
                }
            }
        } catch (SQLException e) {
            cerrarSilenciosamente(fisica);
        }
    }

    private void cerrarSilenciosamente(Connection c) {
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Ejecuta una consulta rápida de validación ({@code SELECT 1}) para comprobar la disponibilidad del servidor.
     *
     * @throws SQLException Si la base de datos no responde correctamente.
     */
    public void probarConexion() throws SQLException {
        try (Connection c = abrirConexion(); var s = c.createStatement(); var rs = s.executeQuery("SELECT 1")) {
            if (!rs.next() || rs.getInt(1) != 1) {
                throw new SQLException("La consulta de comprobación no devolvió 1");
            }
        }
    }

    private String requerido(String key) {
        String value = propiedades.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("tu-") || value.equals("CAMBIAR")) {
            throw new IllegalStateException("Configura " + key + " en config.properties");
        }
        return value;
    }
}
