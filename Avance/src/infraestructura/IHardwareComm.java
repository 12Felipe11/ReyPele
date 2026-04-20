package infraestructura;

import dominio.SensorData;

/**
 * Interfaz de comunicacion con hardware.
 * Coincide con el protocolo serial real del Arduino ReyPele.
 */
public interface IHardwareComm {
    void connect();
    void disconnect();
    boolean isConnected();
    SensorData readSensors();
    boolean setAlarm(boolean on);
    boolean setLight(int intensity);
    boolean setThreshold(float cm);
}
