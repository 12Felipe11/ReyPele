package infraestructura.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Administra la conexion SQLite y el esquema de la base de datos.
 *
 * Archivo generado: estadio.db (en el directorio de ejecucion del proyecto).
 *
 * Dependencia: sqlite-jdbc-x.x.x.jar en el classpath.
 * Descargar: https://github.com/xerial/sqlite-jdbc/releases
 * Agregar en IntelliJ: File > Project Structure > Libraries > "+" > Java > seleccionar el JAR
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:estadio.db";

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            createTables();
            System.out.println("[DB] Base de datos conectada: estadio.db");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] ERROR: sqlite-jdbc.jar no encontrado en classpath.");
            System.err.println("     Descarga: https://github.com/xerial/sqlite-jdbc/releases");
        } catch (SQLException e) {
            System.err.println("[DB] Error al conectar: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // Registro de cada entrada al estadio
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS entry_events (" +
                "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  timestamp   TEXT    NOT NULL DEFAULT (datetime('now','localtime'))," +
                "  sensor_id   TEXT    NOT NULL," +
                "  location    TEXT    NOT NULL," +
                "  entry_count INTEGER NOT NULL" +
                ")"
            );

            // Cambios de modo del sistema (AUTO / MANUAL / EMERGENCIA)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS mode_changes (" +
                "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  timestamp TEXT    NOT NULL DEFAULT (datetime('now','localtime'))," +
                "  mode_name TEXT    NOT NULL" +
                ")"
            );

            // Eventos de alarma (ON / OFF) y capacidad excedida
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS system_events (" +
                "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  timestamp   TEXT    NOT NULL DEFAULT (datetime('now','localtime'))," +
                "  event_type  TEXT    NOT NULL," +
                "  event_data  TEXT" +
                ")"
            );
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Conexion cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al cerrar: " + e.getMessage());
        }
    }
}
