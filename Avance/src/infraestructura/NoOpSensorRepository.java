package infraestructura;

import dominio.SensorData;
import dominio.persistence.ISensorRepository;

/**
 * Implementacion nula del repositorio. Se usa cuando no hay credenciales
 * de Supabase configuradas, para que el sistema siga operando normalmente
 * sin persistencia.
 */
public class NoOpSensorRepository implements ISensorRepository {

    @Override public void saveReading(SensorData data)                       { }
    @Override public void saveEvent(String kind, String payloadJson)         { }
    @Override public void updateConfig(int occupancy, float distance)        { }
}
