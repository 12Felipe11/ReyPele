package dominio.actuator;

import infraestructura.IHardwareComm;

/**
 * Actuador de alarma por zona.
 * Controla la activación y desactivación de alarmas en una zona del estadio.
 */
public class AlarmZoneActuator extends Actuator {

    public AlarmZoneActuator(String id, String zone, IHardwareComm hardwareComm) {
        super(id, zone, hardwareComm);
    }

    @Override
    public void activate() {
        this.active = true;
        hardwareComm.sendCommand(id, "ALARM_ON");
    }

    @Override
    public void deactivate() {
        this.active = false;
        hardwareComm.sendCommand(id, "ALARM_OFF");
    }

    @Override
    public void applyAutoMode() {
        deactivate();
    }

    @Override
    public String getStatusLine() {
        return String.format("  Alarma:      %s", active ? "ACTIVA" : "INACTIVA");
    }
}
