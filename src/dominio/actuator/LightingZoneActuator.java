package dominio.actuator;

import infraestructura.IHardwareComm;

/**
 * Actuador de iluminación por zona.
 * Controla la intensidad de luces en una zona del estadio.
 * En emergencia se activa al 100% para facilitar la evacuación.
 */
public class LightingZoneActuator extends Actuator {

    private int intensityPercent;

    public LightingZoneActuator(String id, String zone, IHardwareComm hardwareComm) {
        super(id, zone, hardwareComm);
        this.intensityPercent = 0;
    }

    @Override
    public void activate() {
        setIntensity(100);
    }

    @Override
    public void deactivate() {
        setIntensity(0);
    }

    public void setIntensity(int percent) {
        this.intensityPercent = percent;
        this.active = (percent > 0);
        hardwareComm.sendCommand(id, "LIGHT_SET:" + percent);
    }

    public int getIntensityPercent() {
        return intensityPercent;
    }
}
