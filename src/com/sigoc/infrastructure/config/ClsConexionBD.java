package com.sigoc.infrastructure.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Centraliza la configuración y apertura de conexiones JDBC. */
public final class ClsConexionBD {
    private static final ClsConexionBD INSTANCIA = new ClsConexionBD();
    private final Properties propiedades = new Properties();

    private ClsConexionBD() {
        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("config.properties"))) {
            propiedades.load(in);
        } catch (java.nio.file.NoSuchFileException ignored) {
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer config.properties", ex);
        }
        try (InputStream in = ClsConexionBD.class.getClassLoader().getResourceAsStream("config.properties.example")) {
            if (in != null) {
                Properties ejemplo = new Properties(); ejemplo.load(in);
                for (String key : ejemplo.stringPropertyNames()) propiedades.putIfAbsent(key, ejemplo.getProperty(key));
            }
        } catch (Exception ex) { throw new IllegalStateException("No se pudo leer configuración de ejemplo", ex); }
    }

    public static ClsConexionBD getInstancia() { return INSTANCIA; }

    public Connection abrirConexion() throws SQLException {
        String url = propiedades.getProperty("db.url");
        if (url == null || url.isBlank()) {
            String host = requerido("db.host"), db = requerido("db.name");
            String ssl = Boolean.parseBoolean(propiedades.getProperty("db.ssl", "true")) ? "true" : "false";
            String zona = propiedades.getProperty("db.timezone", "America/Lima");
            url = "jdbc:mysql://" + host + ":" + propiedades.getProperty("db.port", "3306") + "/" + db
                    + "?useSSL=" + ssl + "&serverTimezone=" + zona;
        }
        String user = requerido("db.user"), password = requerido("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    public void probarConexion() throws SQLException {
        try (Connection c = abrirConexion(); var s = c.createStatement(); var rs = s.executeQuery("SELECT 1")) {
            if (!rs.next() || rs.getInt(1) != 1) throw new SQLException("La consulta de comprobación no devolvió 1");
        }
    }

    private String requerido(String key) {
        String value = propiedades.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("tu-") || value.equals("CAMBIAR"))
            throw new IllegalStateException("Configura " + key + " en config.properties");
        return value;
    }
}
