package dominio.persistence;

import dominio.SensorData;

/**
 * Puerto de persistencia del dominio. Permite guardar lecturas de sensores
 * y eventos del sistema sin acoplar el dominio a una tecnologia concreta
 * (SQLite, memoria, etc.).
 */
public interface ISensorRepository {

    /** Persiste una lectura de sensores (solo cuando hay nueva entrada). */
    void saveReading(SensorData data);

    /**
     * Registra un evento del sistema (alarma, luz, cambio de modo, umbral).
     *
     * @param kind        tipo del evento (ej: "ALARM", "LIGHT", "MODE", "THRESHOLD").
     * @param payloadJson cuerpo JSON ya serializado (ej: {"on":true}).
     */
    void saveEvent(String kind, String payloadJson);

    /** Actualiza la fila de configuracion vigente (estado actual). */
    void updateConfig(int occupancyThreshold, float distanceThreshold);

    /** Guarda un snapshot historico de cada cambio de configuracion. */
    void saveConfigHistory(int occupancyThreshold, float distanceThreshold);

    /** Abre una nueva sesion de sistema e inicia su registro. */
    void openSession(String source);

    /** Cierra la sesion activa registrando la hora de finalizacion. */
    void closeSession();

    /** Registra una nueva entrada en el resumen diario y actualiza el pico. */
    void recordDailyEntry(int currentTotalCount);

    /** Incrementa el contador de alarmas disparadas en el resumen diario. */
    void recordDailyAlarm();

    /** Libera los recursos de conexion. */
    void close();

    // ---- Auditoría y analítica (default → NoOpSensorRepository sin cambios) ----

    /** Registra cualquier acción importante en audit_log. */
    default void logAudit(String action, String description) {}

    /** Registra un cambio de modo en mode_history. */
    default void recordModeHistory(String mode) {}

    /** Abre un nuevo registro en alarm_history con la razón indicada. */
    default void openAlarmHistory(String reason) {}

    /** Cierra el último alarm_history abierto y calcula la duración. */
    default void closeAlarmHistory() {}

    /** Registra una detección individual (HC-SR04) en la tabla entries. */
    default void recordIndividualEntry(double distanceCm) {}

    /** Registra un cambio de iluminación en light_history. */
    default void recordLightHistory(int intensity, String zone) {}

    /**
     * Devuelve un JSON con analítica completa para el dashboard.
     * @param currentCount  aforo actual del sensor
     * @param maxCapacity   umbral de ocupación configurado
     */
    default String getDashboardAnalytics(int currentCount, int maxCapacity) { return "{}"; }

    /**
     * Devuelve JSON con datos históricos de ocupación o alarmas,
     * agrupados por el período indicado (hour|day|week|month).
     */
    default String getHistory(String type, String period) {
        return "{\"labels\":[],\"values\":[]}";
    }
}
