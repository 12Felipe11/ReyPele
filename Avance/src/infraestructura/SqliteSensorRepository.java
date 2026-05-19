package infraestructura;

import dominio.SensorData;
import dominio.persistence.ISensorRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Repositorio local basado en SQLite (archivo .db en disco).
 * Usa el driver org.sqlite.JDBC (sqlite-jdbc-*.jar en lib/).
 * <p>
 * Todas las marcas de tiempo se generan en Java con LocalDateTime.now()
 * para garantizar hora local correcta (SQLite datetime('now') devuelve UTC).
 * <p>
 * Tablas gestionadas:
 * - sensor_readings  : una fila por entrada detectada
 * - events           : eventos del sistema (alarma, luz, modo)
 * - stadium_config   : configuracion vigente (singleton)
 * - config_history   : historial de cada cambio de configuracion
 * - sessions         : registro de cada arranque/cierre del sistema
 * - daily_occupancy  : resumen diario de entradas, pico y alarmas
 */
public class SqliteSensorRepository implements ISensorRepository {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Connection conn;
    private long currentSessionId = -1;

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
                "  read_at TEXT NOT NULL" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS events (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  kind TEXT NOT NULL," +
                "  payload TEXT NOT NULL DEFAULT '{}'," +
                "  occurred_at TEXT NOT NULL" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS stadium_config (" +
                "  id INTEGER PRIMARY KEY," +
                "  occupancy_threshold INTEGER NOT NULL," +
                "  distance_threshold REAL NOT NULL," +
                "  updated_at TEXT NOT NULL" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS config_history (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  occupancy_threshold INTEGER NOT NULL," +
                "  distance_threshold REAL NOT NULL," +
                "  changed_at TEXT NOT NULL" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sessions (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  source TEXT NOT NULL," +
                "  started_at TEXT NOT NULL," +
                "  ended_at TEXT" +
                ")"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS daily_occupancy (" +
                "  date TEXT PRIMARY KEY," +
                "  total_entries INTEGER NOT NULL DEFAULT 0," +
                "  peak_count INTEGER NOT NULL DEFAULT 0," +
                "  alarms_triggered INTEGER NOT NULL DEFAULT 0" +
                ")"
            );
        }
    }

    private String now() {
        return LocalDateTime.now().format(DT_FMT);
    }

    private String today() {
        return LocalDate.now().format(D_FMT);
    }

    @Override
    public void saveReading(SensorData data) {
        final String sql =
            "INSERT INTO sensor_readings " +
            "(entry_count, distance_cm, presence_detected, light_intensity, source, read_at) " +
            "VALUES (?, ?, ?, ?, 'arduino', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, data.getEntryCount());
            ps.setDouble(2, data.getDistanceCm());
            ps.setInt(3, data.isPresenceDetected() ? 1 : 0);
            ps.setInt(4, data.getLightIntensity());
            ps.setString(5, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] saveReading: " + e.getMessage());
        }
    }

    @Override
    public void saveEvent(String kind, String payloadJson) {
        final String sql = "INSERT INTO events (kind, payload, occurred_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kind == null ? "" : kind);
            ps.setString(2, payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson);
            ps.setString(3, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] saveEvent: " + e.getMessage());
        }
    }

    @Override
    public void updateConfig(int occupancyThreshold, float distanceThreshold) {
        final String sql =
            "INSERT INTO stadium_config (id, occupancy_threshold, distance_threshold, updated_at) " +
            "VALUES (1, ?, ?, ?) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "  occupancy_threshold = excluded.occupancy_threshold, " +
            "  distance_threshold  = excluded.distance_threshold, " +
            "  updated_at          = excluded.updated_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, occupancyThreshold);
            ps.setDouble(2, distanceThreshold);
            ps.setString(3, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] updateConfig: " + e.getMessage());
        }
    }

    @Override
    public void saveConfigHistory(int occupancyThreshold, float distanceThreshold) {
        final String sql =
            "INSERT INTO config_history (occupancy_threshold, distance_threshold, changed_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, occupancyThreshold);
            ps.setDouble(2, distanceThreshold);
            ps.setString(3, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] saveConfigHistory: " + e.getMessage());
        }
    }

    @Override
    public void openSession(String source) {
        final String sql = "INSERT INTO sessions (source, started_at) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, source == null ? "unknown" : source);
            ps.setString(2, now());
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) currentSessionId = keys.getLong(1);
            }
            System.out.println("  [SQLITE] Sesion iniciada (id=" + currentSessionId + ", fuente=" + source + ")");
        } catch (SQLException e) {
            System.err.println("  [SQLITE] openSession: " + e.getMessage());
        }
    }

    @Override
    public void closeSession() {
        if (currentSessionId < 0) return;
        final String sql = "UPDATE sessions SET ended_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, now());
            ps.setLong(2, currentSessionId);
            ps.executeUpdate();
            System.out.println("  [SQLITE] Sesion cerrada (id=" + currentSessionId + ")");
            currentSessionId = -1;
        } catch (SQLException e) {
            System.err.println("  [SQLITE] closeSession: " + e.getMessage());
        }
    }

    @Override
    public void recordDailyEntry(int currentTotalCount) {
        final String sql =
            "INSERT INTO daily_occupancy (date, total_entries, peak_count) " +
            "VALUES (?, 1, ?) " +
            "ON CONFLICT(date) DO UPDATE SET " +
            "  total_entries = total_entries + 1," +
            "  peak_count    = MAX(peak_count, excluded.peak_count)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today());
            ps.setInt(2, currentTotalCount);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] recordDailyEntry: " + e.getMessage());
        }
    }

    @Override
    public void recordDailyAlarm() {
        final String sql =
            "INSERT INTO daily_occupancy (date, total_entries, peak_count, alarms_triggered) " +
            "VALUES (?, 0, 0, 1) " +
            "ON CONFLICT(date) DO UPDATE SET " +
            "  alarms_triggered = alarms_triggered + 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, today());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] recordDailyAlarm: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        closeSession();
        try { if (conn != null) conn.close(); }
        catch (SQLException ignored) { }
    }
}
