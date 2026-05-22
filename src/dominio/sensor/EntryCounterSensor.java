package dominio.sensor;

/**
 * Sensor contador de entradas (HU-03).
 * Mantiene el conteo de personas que ingresaron al estadio.
 */
public class EntryCounterSensor extends Sensor {

    private int count;

    public EntryCounterSensor(String id, String location) {
        super(id, location);
        this.count = 0;
    }

    public void updateValue(int newCount) { this.count = newCount; }
    public int getCount() { return count; }
}
