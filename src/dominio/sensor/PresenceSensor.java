package dominio.sensor;

import infraestructura.IHardwareComm;

/**
 * Sensor de presencia/movimiento.
 * Con hardware real: usa los datos de distancia del HC-SR04 enviados por Arduino.
 * Con simulador: devuelve el valor que retorne SimulatorAdapter.
 */
public class PresenceSensor extends Sensor {

    private boolean presenceDetected;

    public PresenceSensor(String id, String location, IHardwareComm hardwareComm) {
        super(id, location, hardwareComm);
        this.presenceDetected = false;
    }

    @Override
    public int readValue() {
        int val = hardwareComm.readInt(id);
        presenceDetected = val == 1;
        return val;
    }

    public boolean isPresenceDetected() {
        return presenceDetected;
    }

    @Override
    public String getStatusLine() {
        return String.format("  Movimiento:  %s", presenceDetected ? "DETECTADO" : "SIN MOVIMIENTO");
    }
}
