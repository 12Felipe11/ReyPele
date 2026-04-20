package dominio;

/**
 * Lectura de sensores del estadio.
 * Parsea el protocolo real: DATA:entradas,distancia,presencia(0|1),luzIntensidad
 */
public class SensorData {

    private final int entryCount;
    private final float distanceCm;
    private final boolean presenceDetected;
    private final int lightIntensity;

    public SensorData(int entryCount, float distanceCm, boolean presenceDetected,
                      int lightIntensity) {
        this.entryCount = entryCount;
        this.distanceCm = distanceCm;
        this.presenceDetected = presenceDetected;
        this.lightIntensity = lightIntensity;
    }

    public int getEntryCount()         { return entryCount; }
    public float getDistanceCm()       { return distanceCm; }
    public boolean isPresenceDetected(){ return presenceDetected; }
    public int getLightIntensity()     { return lightIntensity; }

    /** Parsea "DATA:12,24.5,1,75" */
    public static SensorData parse(String response) {
        int entry = 0;
        float dist = 999.0f;
        boolean pres = false;
        int light = 0;
        try {
            String data = response.startsWith("DATA:") ? response.substring(5) : response;
            String[] parts = data.split(",");
            if (parts.length >= 4) {
                entry = Integer.parseInt(parts[0].trim());
                dist  = Float.parseFloat(parts[1].trim());
                pres  = "1".equals(parts[2].trim());
                light = Integer.parseInt(parts[3].trim());
            }
        } catch (Exception e) {
            System.err.println("  [WARN] Error parseando: " + response);
        }
        return new SensorData(entry, dist, pres, light);
    }
}
