package com.mycompany.finanz.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Punto único de acceso a la base de datos (patrón Singleton).
 *
 * Usa un pool de conexiones (HikariCP): las conexiones se abren una vez y se
 * reutilizan, por eso el sistema soporta muchos usuarios simultáneos sin abrir
 * una conexión nueva en cada consulta. Los DAO siguen llamando a conectar() y
 * cerrando la conexión con try-with-resources (al cerrarla vuelve al pool).
 *
 * Los datos de conexión se leen, en este orden:
 *   1. Variables de entorno FINANZ_DB_URL, FINANZ_DB_USER, FINANZ_DB_PASSWORD
 *   2. El archivo src/main/resources/db.properties (no se sube a GitHub)
 *   3. Valores por defecto: localhost:3306/db_finanz con usuario root
 */
public final class ConexionDB {

    private static final String URL_POR_DEFECTO = "jdbc:mysql://localhost:3306/db_finanz"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&characterEncoding=UTF-8";

    private static volatile HikariDataSource dataSource;

    private ConexionDB() {
    }

    public static Connection conectar() throws SQLException {
        return obtenerDataSource().getConnection();
    }

    private static HikariDataSource obtenerDataSource() {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            synchronized (ConexionDB.class) {
                ds = dataSource;
                if (ds == null) {
                    ds = crearDataSource();
                    dataSource = ds;
                }
            }
        }
        return ds;
    }

    private static HikariDataSource crearDataSource() {
        Properties archivo = leerArchivoPropiedades();

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(valor("FINANZ_DB_URL", archivo, "db.url", URL_POR_DEFECTO));
        config.setUsername(valor("FINANZ_DB_USER", archivo, "db.usuario", "root"));
        config.setPassword(valor("FINANZ_DB_PASSWORD", archivo, "db.clave", ""));
        config.setMaximumPoolSize(Integer.parseInt(valor("FINANZ_DB_POOL", archivo, "db.pool", "20")));
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setPoolName("FinanzPool");
        // Cachea las sentencias preparadas en el driver (mejor rendimiento)
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        // Si la BD aún no está levantada, el pool se crea igual y reintenta en cada conectar()
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private static Properties leerArchivoPropiedades() {
        Properties props = new Properties();
        try (InputStream in = ConexionDB.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer db.properties: " + e.getMessage());
        }
        return props;
    }

    private static String valor(String variableEntorno, Properties archivo, String clave, String porDefecto) {
        String env = System.getenv(variableEntorno);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return archivo.getProperty(clave, porDefecto);
    }

    /** Cierra el pool al detener la aplicación (lo llama AppListener). */
    public static synchronized void cerrar() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
