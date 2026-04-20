package infraestructura.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para los eventos de entrada al estadio.
 * Sigue el patron Repository para aislar la logica de persistencia del dominio.
 */
public class EntryRepository {

    private final DatabaseManager dbManager;

    public EntryRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // ---------------------------------------------------------------
    // Escritura
    // ---------------------------------------------------------------

    /** Persiste un nuevo evento de entrada. */
    public void saveEntry(String sensorId, String location, int totalCount) {
        if (!dbManager.isConnected()) return;

        String sql = "INSERT INTO entry_events (sensor_id, location, entry_count) VALUES (?, ?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, sensorId);
            ps.setString(2, location);
            ps.setInt(3, totalCount);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error al guardar entrada: " + e.getMessage());
        }
    }

    /** Persiste un evento de sistema (ej: CAPACITY_EXCEEDED, ALARM_ON). */
    public void saveSystemEvent(String eventType, String eventData) {
        if (!dbManager.isConnected()) return;

        String sql = "INSERT INTO system_events (event_type, event_data) VALUES (?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, eventType);
            ps.setString(2, eventData);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error al guardar evento: " + e.getMessage());
        }
    }

    /** Persiste un cambio de modo de operacion. */
    public void saveModeChange(String modeName) {
        if (!dbManager.isConnected()) return;

        String sql = "INSERT INTO mode_changes (mode_name) VALUES (?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, modeName);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Error al guardar cambio de modo: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Lectura
    // ---------------------------------------------------------------

    /** Retorna el total de entradas registradas hoy. */
    public int getTotalEntriesToday() {
        if (!dbManager.isConnected()) return 0;

        String sql = "SELECT COUNT(*) FROM entry_events " +
                     "WHERE date(timestamp) = date('now','localtime')";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DB] Error al consultar entradas de hoy: " + e.getMessage());
        }
        return 0;
    }

    /** Retorna el ultimo conteo registrado para un sensor. */
    public int getLastCountForSensor(String sensorId) {
        if (!dbManager.isConnected()) return 0;

        String sql = "SELECT entry_count FROM entry_events " +
                     "WHERE sensor_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, sensorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("entry_count");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al consultar ultimo conteo: " + e.getMessage());
        }
        return 0;
    }

    /** Retorna los ultimos N eventos de entrada. */
    public List<String> getRecentEntries(int limit) {
        List<String> results = new ArrayList<>();
        if (!dbManager.isConnected()) return results;

        String sql = "SELECT timestamp, sensor_id, location, entry_count " +
                     "FROM entry_events ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(String.format("[%s] %s @ %s → total: %d",
                            rs.getString("timestamp"),
                            rs.getString("sensor_id"),
                            rs.getString("location"),
                            rs.getInt("entry_count")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al consultar entradas recientes: " + e.getMessage());
        }
        return results;
    }

    /** Imprime un resumen de la sesion actual en consola. */
    public void printSessionSummary() {
        if (!dbManager.isConnected()) {
            System.out.println("[DB] Sin conexion, no hay resumen disponible.");
            return;
        }
        System.out.println("\n=== RESUMEN DE SESION (BD) ===");
        System.out.println("Entradas registradas hoy: " + getTotalEntriesToday());
        System.out.println("Ultimos 5 eventos:");
        for (String e : getRecentEntries(5)) {
            System.out.println("  " + e);
        }
        System.out.println("==============================\n");
    }
}
