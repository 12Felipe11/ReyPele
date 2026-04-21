package infraestructura;

import dominio.SensorData;
import dominio.persistence.ISensorRepository;

/**
 * Implementacion nula del repositorio. Se usa cuando no hay base de datos
 * disponible, para que el sistema siga operando sin persistencia.
 */
public class NoOpSensorRepository implements ISensorRepository {

    @Override public void saveReading(SensorData data)                        { }
    @Override public void saveEvent(String kind, String payloadJson)          { }
    @Override public void updateConfig(int occupancy, float distance)         { }
    @Override public void saveConfigHistory(int occupancy, float distance)    { }
    @Override public void openSession(String source)                          { }
    @Override public void closeSession()                                      { }
    @Override public void recordDailyEntry(int currentTotalCount)             { }
    @Override public void recordDailyAlarm()                                  { }
    @Override public void close()                                             { }
}
