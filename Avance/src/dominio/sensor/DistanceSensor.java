package dominio.sensor;

/**
 * Sensor ultrasonico HC-SR04.
 * Mide distancia en cm y determina presencia cuando distancia < umbral.
 */
public class DistanceSensor extends Sensor {

    private float distanceCm;
    private boolean presenceDetected;

    public DistanceSensor(String id, String location) {
        super(id, location);
        this.distanceCm = 999.0f;
        this.presenceDetected = false;
    }

    public void updateValue(float distance, boolean presence) {
        this.distanceCm = distance;
        this.presenceDetected = presence;
    }

    public float getDistanceCm()        { return distanceCm; }
    public boolean isPresenceDetected() { return presenceDetected; }

    public String getDistanceDescription() {
        if (distanceCm >= 400) return "Fuera de rango";
        if (distanceCm < 10)   return "MUY CERCA";
        if (distanceCm < 30)   return "CERCA";
        if (distanceCm < 100)  return "MEDIO";
        return "LEJOS";
    }
}
