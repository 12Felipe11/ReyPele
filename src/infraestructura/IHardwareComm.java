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

    /** Modos del estadio (WS2812B-8): OFF, PARTIDO, ENTRENAMIENTO, EVENTO, EMERGENCIA, MANUAL. */
    boolean setStadiumMode(String mode);

    /** Color de una zona (entra en modo MANUAL automaticamente).
     *  Zonas validas: NORTE, ORIENTAL, CANCHA, OCCIDENTAL, SUR. */
    boolean setZoneColor(String zone, int r, int g, int b);

    /** Apaga una zona (entra en modo MANUAL automaticamente). */
    boolean setZoneOff(String zone);
}
