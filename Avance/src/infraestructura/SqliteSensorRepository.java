package infraestructura;

import dominio.SensorData;
import dominio.persistence.ISensorRepository;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Repositorio SQLite completo: lecturas, eventos, configuración, sesiones,
 * ocupación diaria, auditoría, historial de modos/alarmas/iluminación y
 * registro individual de ingresos.
 *
 * Tablas:
 *   sensor_readings  – polling de sensores por cada nueva entrada detectada
 *   events           – eventos genéricos del sistema
 *   stadium_config   – configuración vigente (singleton id=1)
 *   config_history   – historial de cambios de configuración
 *   sessions         – registro de cada arranque y cierre
 *   daily_occupancy  – resumen diario (entradas, pico, alarmas)
 *   audit_log        – trazabilidad de acciones importantes
 *   mode_history     – cada cambio de modo de operación
 *   alarm_history    – ciclos de alarma con duración calculada
 *   entries          – cada persona detectada individualmente
 *   light_history    – historial de cambios de iluminación
 */
public class SqliteSensorRepository implements ISensorRepository {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Connection conn;
    private long currentSessionId = -1;
    private long currentAlarmId   = -1;

    public SqliteSensorRepository(String dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initSchema();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("No se pudo abrir SQLite en " + dbPath, e);
        }
    }

    // ============================================================= SCHEMA

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            // tablas originales
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sensor_readings (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  entry_count INTEGER NOT NULL," +
                "  distance_cm REAL NOT NULL," +
                "  presence_detected INTEGER NOT NULL," +
                "  light_intensity INTEGER NOT NULL," +
                "  source TEXT NOT NULL DEFAULT 'arduino'," +
                "  read_at TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS events (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  kind TEXT NOT NULL," +
                "  payload TEXT NOT NULL DEFAULT '{}'," +
                "  occurred_at TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS stadium_config (" +
                "  id INTEGER PRIMARY KEY," +
                "  occupancy_threshold INTEGER NOT NULL," +
                "  distance_threshold REAL NOT NULL," +
                "  updated_at TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS config_history (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  occupancy_threshold INTEGER NOT NULL," +
                "  distance_threshold REAL NOT NULL," +
                "  changed_at TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sessions (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  source TEXT NOT NULL," +
                "  started_at TEXT NOT NULL," +
                "  ended_at TEXT)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS daily_occupancy (" +
                "  date TEXT PRIMARY KEY," +
                "  total_entries INTEGER NOT NULL DEFAULT 0," +
                "  peak_count INTEGER NOT NULL DEFAULT 0," +
                "  alarms_triggered INTEGER NOT NULL DEFAULT 0)");
            // nuevas tablas
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS audit_log (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  action TEXT NOT NULL," +
                "  description TEXT," +
                "  timestamp TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS mode_history (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  mode TEXT NOT NULL," +
                "  activated_at TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS alarm_history (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  reason TEXT," +
                "  activated_at TEXT NOT NULL," +
                "  deactivated_at TEXT," +
                "  duration_seconds INTEGER)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS entries (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  detected_at TEXT NOT NULL," +
                "  distance_cm REAL," +
                "  session_id INTEGER)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS light_history (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  intensity INTEGER NOT NULL," +
                "  zone TEXT NOT NULL," +
                "  timestamp TEXT NOT NULL)");
        }
    }

    // ============================================================= UTILS

    private String now()   { return LocalDateTime.now().format(DT_FMT); }
    private String today() { return LocalDate.now().format(D_FMT); }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private int queryInt(String sql, String param) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ignored) { return 0; }
    }

    // ============================================================= ORIGINAL METHODS

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
        // Detectar sesiones anteriores no cerradas (reinicio abrupto)
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM sessions WHERE ended_at IS NULL")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                logAudit("SYSTEM_RESTART",
                    "Reinicio detectado: sesión anterior no cerrada correctamente");
            }
        } catch (SQLException ignored) {}

        final String sql = "INSERT INTO sessions (source, started_at) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, source == null ? "unknown" : source);
            ps.setString(2, now());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) currentSessionId = keys.getLong(1);
            }
            logAudit("SESSION_START", "Sistema iniciado: " + source);
            System.out.println("  [SQLITE] Sesion iniciada (id=" + currentSessionId + ", fuente=" + source + ")");
        } catch (SQLException e) {
            System.err.println("  [SQLITE] openSession: " + e.getMessage());
        }
    }

    @Override
    public void closeSession() {
        if (currentSessionId < 0) return;
        generateSessionReport();
        logAudit("SESSION_END", "Sistema cerrado correctamente. Sesión id=" + currentSessionId);
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
        try { if (conn != null && !conn.isClosed()) conn.close(); }
        catch (SQLException ignored) {}
    }

    // ============================================================= AUDIT & ANALYTICS

    @Override
    public void logAudit(String action, String description) {
        final String sql = "INSERT INTO audit_log (action, description, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action == null ? "" : action);
            ps.setString(2, description == null ? "" : description);
            ps.setString(3, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] logAudit: " + e.getMessage());
        }
    }

    @Override
    public void recordModeHistory(String mode) {
        final String sql = "INSERT INTO mode_history (mode, activated_at) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mode == null ? "" : mode);
            ps.setString(2, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] recordModeHistory: " + e.getMessage());
        }
    }

    @Override
    public void openAlarmHistory(String reason) {
        final String sql = "INSERT INTO alarm_history (reason, activated_at) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reason == null ? "Manual" : reason);
            ps.setString(2, now());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) currentAlarmId = keys.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("  [SQLITE] openAlarmHistory: " + e.getMessage());
        }
    }

    @Override
    public void closeAlarmHistory() {
        if (currentAlarmId < 0) return;
        try {
            String activatedAt = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT activated_at FROM alarm_history WHERE id=?")) {
                ps.setLong(1, currentAlarmId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) activatedAt = rs.getString(1);
            }
            int durationSecs = 0;
            if (activatedAt != null) {
                try {
                    durationSecs = (int) Duration.between(
                        LocalDateTime.parse(activatedAt, DT_FMT), LocalDateTime.now()).getSeconds();
                } catch (Exception ignored) {}
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE alarm_history SET deactivated_at=?, duration_seconds=? WHERE id=?")) {
                ps.setString(1, now());
                ps.setInt(2, durationSecs);
                ps.setLong(3, currentAlarmId);
                ps.executeUpdate();
            }
            currentAlarmId = -1;
        } catch (SQLException e) {
            System.err.println("  [SQLITE] closeAlarmHistory: " + e.getMessage());
        }
    }

    @Override
    public void recordIndividualEntry(double distanceCm) {
        final String sql = "INSERT INTO entries (detected_at, distance_cm, session_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, now());
            ps.setDouble(2, distanceCm);
            if (currentSessionId >= 0) ps.setLong(3, currentSessionId);
            else                       ps.setNull(3, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] recordIndividualEntry: " + e.getMessage());
        }
    }

    @Override
    public void recordLightHistory(int intensity, String zone) {
        final String sql = "INSERT INTO light_history (intensity, zone, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, intensity);
            ps.setString(2, zone == null ? "ALL" : zone);
            ps.setString(3, now());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [SQLITE] recordLightHistory: " + e.getMessage());
        }
    }

    // ============================================================= ANALYTICS QUERY

    @Override
    public String getDashboardAnalytics(int currentCount, int maxCapacity) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append('{');
        String td          = today();
        String fiveMinsAgo = LocalDateTime.now().minusMinutes(5).format(DT_FMT);
        String oneHourAgo  = LocalDateTime.now().minusHours(1).format(DT_FMT);

        // --- audit_log (últimas 20 entradas) ---
        sb.append("\"auditLog\":[");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, COALESCE(description,'') AS description, timestamp " +
                "FROM audit_log ORDER BY id DESC LIMIT 20")) {
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(',');
                first = false;
                String ts   = rs.getString("timestamp");
                String time = (ts != null && ts.length() >= 19) ? ts.substring(11, 19) : (ts != null ? ts : "");
                sb.append("{\"timestamp\":\"").append(esc(time)).append("\",");
                sb.append("\"action\":\"").append(esc(rs.getString("action"))).append("\",");
                sb.append("\"description\":\"").append(esc(rs.getString("description"))).append("\"}");
            }
        } catch (SQLException ignored) {}
        sb.append("],");

        // --- today stats ---
        int todayEntries = queryInt(
            "SELECT COALESCE(total_entries,0) FROM daily_occupancy WHERE date=?", td);
        int peakCount    = queryInt(
            "SELECT COALESCE(peak_count,0) FROM daily_occupancy WHERE date=?", td);
        int totalAlarms  = queryInt(
            "SELECT COUNT(*) FROM alarm_history WHERE substr(activated_at,1,10)=?", td);

        String peakHour        = "--";
        int    peakHourEntries = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT substr(detected_at,12,2) AS hr, COUNT(*) AS cnt FROM entries " +
                "WHERE substr(detected_at,1,10)=? GROUP BY hr ORDER BY cnt DESC LIMIT 1")) {
            ps.setString(1, td);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                peakHour        = rs.getString("hr") + ":00";
                peakHourEntries = rs.getInt("cnt");
            }
        } catch (SQLException ignored) {}

        int operatingMinutes = 0;
        if (currentSessionId >= 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT started_at FROM sessions WHERE id=?")) {
                ps.setLong(1, currentSessionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getString(1) != null) {
                    try {
                        operatingMinutes = (int) Duration.between(
                            LocalDateTime.parse(rs.getString(1), DT_FMT),
                            LocalDateTime.now()).toMinutes();
                    } catch (Exception ignored) {}
                }
            } catch (SQLException ignored) {}
        }

        sb.append("\"todayStats\":{");
        sb.append("\"totalEntries\":").append(todayEntries).append(',');
        sb.append("\"peakHour\":\"").append(esc(peakHour)).append("\",");
        sb.append("\"peakHourEntries\":").append(peakHourEntries).append(',');
        sb.append("\"peakCount\":").append(peakCount).append(',');
        sb.append("\"totalAlarms\":").append(totalAlarms).append(',');
        sb.append("\"operatingMinutes\":").append(operatingMinutes);
        sb.append("},");

        // --- flow stats ---
        int    flowLast5Min  = queryInt(
            "SELECT COUNT(*) FROM entries WHERE detected_at >= ?", fiveMinsAgo);
        int    flowLastHour  = queryInt(
            "SELECT COUNT(*) FROM entries WHERE detected_at >= ?", oneHourAgo);
        double flowPerMinute = flowLast5Min / 5.0;

        double histAvg = 0.0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT AVG(total_entries) FROM daily_occupancy WHERE total_entries > 0")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) histAvg = rs.getDouble(1) / (8.0 * 60.0);
        } catch (SQLException ignored) {}

        int predictionMinutes = -1;
        if (currentCount >= maxCapacity && maxCapacity > 0) {
            predictionMinutes = 0;
        } else if (flowPerMinute > 0 && maxCapacity > 0) {
            predictionMinutes = (int) Math.ceil((maxCapacity - currentCount) / flowPerMinute);
        }

        sb.append("\"flow\":{");
        sb.append("\"perMinute\":").append(String.format(Locale.US, "%.1f", flowPerMinute)).append(',');
        sb.append("\"perHour\":").append(flowLastHour).append(',');
        sb.append("\"historicalAvgPerMinute\":").append(String.format(Locale.US, "%.2f", histAvg)).append(',');
        sb.append("\"capacityPredictionMinutes\":").append(predictionMinutes);
        sb.append("},");

        // --- mode distribution ---
        sb.append("\"modeStats\":[");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT mode, COUNT(*) AS n FROM mode_history GROUP BY mode ORDER BY n DESC")) {
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(',');
                first = false;
                sb.append("{\"mode\":\"").append(esc(rs.getString("mode"))).append("\",");
                sb.append("\"count\":").append(rs.getInt("n")).append('}');
            }
        } catch (SQLException ignored) {}
        sb.append("],");

        // --- light stats ---
        int avgIntensity = 0, maxIntensity = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CAST(COALESCE(AVG(intensity),0) AS INTEGER), COALESCE(MAX(intensity),0) " +
                "FROM light_history WHERE substr(timestamp,1,10)=?")) {
            ps.setString(1, td);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { avgIntensity = rs.getInt(1); maxIntensity = rs.getInt(2); }
        } catch (SQLException ignored) {}

        sb.append("\"lightStats\":{");
        sb.append("\"avgIntensity\":").append(avgIntensity).append(',');
        sb.append("\"maxIntensity\":").append(maxIntensity);
        sb.append('}');

        sb.append('}');
        return sb.toString();
    }

    // ============================================================= HISTORY QUERY

    @Override
    public String getHistory(String type, String period) {
        List<String>  labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        boolean isOcc  = "occupancy".equals(type);
        boolean isMonth = "month".equals(period);

        String sql;
        String param = null;

        if (isOcc) {
            switch (period) {
                case "hour":
                    sql   = "SELECT substr(detected_at,12,2) AS lb, COUNT(*) AS v " +
                            "FROM entries WHERE substr(detected_at,1,10)=? " +
                            "GROUP BY lb ORDER BY lb";
                    param = today();
                    break;
                case "day":
                    sql   = "SELECT date AS lb, COALESCE(total_entries,0) AS v " +
                            "FROM daily_occupancy WHERE date >= ? ORDER BY date";
                    param = LocalDate.now().minusDays(6).format(D_FMT);
                    break;
                case "week":
                    sql   = "SELECT strftime('%Y-W%W', date) AS lb, SUM(total_entries) AS v " +
                            "FROM daily_occupancy WHERE date >= ? GROUP BY lb ORDER BY lb";
                    param = LocalDate.now().minusWeeks(3).format(D_FMT);
                    break;
                default: // month
                    sql   = "SELECT substr(date,1,7) AS lb, SUM(total_entries) AS v " +
                            "FROM daily_occupancy GROUP BY lb ORDER BY lb DESC LIMIT 12";
                    break;
            }
        } else { // alarms
            switch (period) {
                case "hour":
                    sql   = "SELECT substr(activated_at,12,2) AS lb, COUNT(*) AS v " +
                            "FROM alarm_history WHERE substr(activated_at,1,10)=? " +
                            "GROUP BY lb ORDER BY lb";
                    param = today();
                    break;
                case "day":
                    sql   = "SELECT substr(activated_at,1,10) AS lb, COUNT(*) AS v " +
                            "FROM alarm_history WHERE substr(activated_at,1,10) >= ? " +
                            "GROUP BY lb ORDER BY lb";
                    param = LocalDate.now().minusDays(6).format(D_FMT);
                    break;
                case "week":
                    sql   = "SELECT strftime('%Y-W%W', substr(activated_at,1,10)) AS lb, COUNT(*) AS v " +
                            "FROM alarm_history WHERE substr(activated_at,1,10) >= ? " +
                            "GROUP BY lb ORDER BY lb";
                    param = LocalDate.now().minusWeeks(3).format(D_FMT);
                    break;
                default: // month
                    sql   = "SELECT substr(activated_at,1,7) AS lb, COUNT(*) AS v " +
                            "FROM alarm_history GROUP BY lb ORDER BY lb DESC LIMIT 12";
                    break;
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                labels.add(rs.getString("lb"));
                values.add(rs.getInt("v"));
            }
        } catch (SQLException e) {
            System.err.println("  [SQLITE] getHistory: " + e.getMessage());
        }

        if (isMonth) {
            Collections.reverse(labels);
            Collections.reverse(values);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"labels\":[");
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(esc(labels.get(i))).append('"');
        }
        sb.append("],\"values\":[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(values.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    // ============================================================= SESSION REPORT

    private void generateSessionReport() {
        try {
            String td = today();
            int durationMins = 0;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT started_at FROM sessions WHERE id=?")) {
                ps.setLong(1, currentSessionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getString(1) != null) {
                    try {
                        durationMins = (int) Duration.between(
                            LocalDateTime.parse(rs.getString(1), DT_FMT),
                            LocalDateTime.now()).toMinutes();
                    } catch (Exception ignored) {}
                }
            }

            int totalEntries  = queryInt(
                "SELECT COALESCE(total_entries,0) FROM daily_occupancy WHERE date=?", td);
            int maxOccupancy  = queryInt(
                "SELECT COALESCE(MAX(entry_count),0) FROM sensor_readings WHERE substr(read_at,1,10)=?", td);
            int totalAlarms   = queryInt(
                "SELECT COUNT(*) FROM alarm_history WHERE substr(activated_at,1,10)=?", td);
            int emergencySecs = queryInt(
                "SELECT COALESCE(SUM(COALESCE(duration_seconds,0)),0) FROM alarm_history " +
                "WHERE substr(activated_at,1,10)=?", td);
            int avgLight      = queryInt(
                "SELECT CAST(COALESCE(AVG(intensity),0) AS INTEGER) FROM light_history " +
                "WHERE substr(timestamp,1,10)=?", td);

            String predominantMode = "MANUAL";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT mode FROM mode_history GROUP BY mode ORDER BY COUNT(*) DESC LIMIT 1")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) predominantMode = rs.getString("mode");
            } catch (SQLException ignored) {}

            String report = String.format(
                "{\"fecha\":\"%s\",\"duracion_minutos\":%d,\"total_ingresos\":%d," +
                "\"maxima_ocupacion\":%d,\"total_alarmas\":%d,\"modo_predominante\":\"%s\"," +
                "\"tiempo_alarma_segundos\":%d,\"promedio_iluminacion\":%d}",
                td, durationMins, totalEntries, maxOccupancy, totalAlarms,
                predominantMode, emergencySecs, avgLight);

            saveEvent("SESSION_REPORT", report);
            System.out.printf(
                "  [SQLITE] Reporte de sesion: fecha=%s dur=%d min ingresos=%d " +
                "maxOcup=%d alarmas=%d modo=%s luz=%d%%%n",
                td, durationMins, totalEntries, maxOccupancy, totalAlarms,
                predominantMode, avgLight);
        } catch (Exception e) {
            System.err.println("  [SQLITE] generateSessionReport: " + e.getMessage());
        }
    }
}
