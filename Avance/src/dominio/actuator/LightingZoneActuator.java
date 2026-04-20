package dominio.actuator;

/** Actuador de luces con intensidad 0-100% (HU-04, HU-05). */
public class LightingZoneActuator extends Actuator {

    private int intensity;

    public LightingZoneActuator(String id, String zone) {
        super(id, zone);
        this.intensity = 0;
    }

    public int getIntensity()          { return intensity; }
    public void setIntensity(int val)  { this.intensity = Math.max(0, Math.min(100, val)); }
}
