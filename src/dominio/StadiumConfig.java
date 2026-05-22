package dominio;

/**
 * Configuracion del estadio.
 * - occupancyThreshold: limite de personas para alarma (HU-12)
 * - distanceThreshold: umbral del HC-SR04 en cm (SET_THRESHOLD)
 */
public class StadiumConfig {

    private int occupancyThreshold;
    private float distanceThreshold;

    public StadiumConfig() {
        this.occupancyThreshold = 100;
        this.distanceThreshold = 30.0f;
    }

    public int getOccupancyThreshold()           { return occupancyThreshold; }
    public void setOccupancyThreshold(int val)   { if (val > 0) occupancyThreshold = val; }

    public float getDistanceThreshold()          { return distanceThreshold; }
    public void setDistanceThreshold(float cm)   { if (cm > 0) distanceThreshold = cm; }
}
