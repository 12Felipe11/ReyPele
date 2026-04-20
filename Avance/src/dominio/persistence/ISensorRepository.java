package dominio.persistence;

import dominio.SensorData;

/**
 * Puerto de persistencia del dominio. Permite guardar lecturas de sensores
 * y eventos del sistema sin acoplar el dominio a una tecnologia concreta
 * (Supabase, archivo local, memoria, etc.).
 */
public interface ISensorRepository {

    /** Persiste una lectura de sensores. */
    void saveReading(SensorData data);

    /**
     * Registra un evento del sistema (alarma, luz, cambio de modo, umbral).
     *
     * @param kind        tipo del evento (ej: "ALARM", "LIGHT", "MODE", "THRESHOLD").
     * @param payloadJson cuerpo JSON ya serializado (ej: {"on":true}).
     */
    void saveEvent(String kind, String payloadJson);

    /** Actualiza la fila de configuracion vigente. */
    void updateConfig(int occupancyThreshold, float distanceThreshold);
}
