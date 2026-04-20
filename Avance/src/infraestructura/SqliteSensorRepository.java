package infraestructura;

import dominio.SensorData;
import dominio.persistence.ISensorRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Repositorio local basado en SQLite (archivo .db en disco).
 * Usa el driver org.sqlite.JDBC (sqlite-jdbc-*.jar en lib/).
 * <p>
 * Crea automaticamente las tablas {@code sensor_readings}, {@code events}
 * y {@code stadium_config} si no existen.
 */
public class SqliteSensorRepository implements ISensorRepository {

    private final Connection conn;

    public SqliteSensorRepository(String dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initSchema();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("No se pudo abrir SQLite en " + dbPath, e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sensor_readings (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  entry_count INTEGER NOT NULL," +
                "  distance_cm REAL NOT NULL," +
                "  presence_detected INTEGER NOT NULL," +
                "  light_intensity INTEGER NOT NULL," +
                "  source TEXT NOT NULL DEFAULT 'arduino'," +
                "  read_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS events (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  kind TEXT NOT NULL," +
                "  payload TEXT NOT NULL DEFAULT '{}'," +
                "  occurred_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS stadium_config (" +
                "  id INTEGER PRIMARY KEY," +
                "  occupancy_threshold INTEGER NOT NULL," +
                "  distance_threshold REAL NOT NULL," +
                "  updated_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                ")"
            );
        }
    }

    @Override
    public void saveReading(SensorData data) {
        final String sql =
            "INSERT INTO sensor_readings " +
            "(entry_count, distance_cm, presence_detected, light_intensity, source) " +
            "VALUES (?, ?, ?, ?, 'arduino')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, data.getEntryCount());
            ps.setDouble(2, data.getDistanceCm());
            ps.setInt(3, data.isPresenceDetected() ? 1 : 0);
            ps.setInt(4, data.getLightIntensity());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] saveReading: " + e.getMessage());
        }
    }

    @Override
    public void saveEvent(String kind, String payloadJson) {
        final String sql = "INSERT INTO events (kind, payload) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kind == null ? "" : kind);
            ps.setString(2, payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] saveEvent: " + e.getMessage());
        }
    }

    @Override
    public void updateConfig(int occupancyThreshold, float distanceThreshold) {
        final String sql =
            "INSERT INTO stadium_config (id, occupancy_threshold, distance_threshold, updated_at) " +
            "VALUES (1, ?, ?, datetime('now')) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "  occupancy_threshold = excluded.occupancy_threshold, " +
            "  distance_threshold  = excluded.distance_threshold, " +
            "  updated_at          = excluded.updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, occupancyThreshold);
            ps.setDouble(2, distanceThreshold);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] updateConfig: " + e.getMessage());
        }
    }

    public void close() {
        try { if (conn != null) conn.close(); }
        catch (SQLException ignored) { }
    }
}
