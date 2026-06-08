package infraestructura;

import dominio.SensorData;

/**
 * Interfaz de comunicacion con hardware.
 * Coincide con el protocolo serial real del Arduino ReyPele.
 * Los metodos de zona tienen implementacion por defecto para
 * compatibilidad con hardware que no soporte comandos por zona.
 */
public interface IHardwareComm {
    void connect();
    void disconnect();
    boolean isConnected();
    SensorData readSensors();
    boolean setAlarm(boolean on);
    boolean setLight(int intensity);
    boolean setThreshold(float cm);

    /** Intensidad de una zona especifica (0-100). */
    default boolean setZoneLight(String zone, int intensity) { return setLight(intensity); }

    /** Color RGB de una zona (0-255 cada componente). */
    default boolean setZoneColor(String zone, int r, int g, int b) { return true; }
}
