package infraestructura;

/**
 * Interfaz de comunicación con hardware.
 * Abstrae la capa física para permitir simulación o conexión real.
 */
public interface IHardwareComm {

    int readInt(String sensorId);

    boolean sendCommand(String deviceId, String command);
}
