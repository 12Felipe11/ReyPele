package dominio.sensor;

import infraestructura.IHardwareComm;

/**
 * Monitorea la cantidad de personas que ingresan al estadio y notifica
 * a los observadores cuando se alcanza la capacidad máxima.
 */
public class EntryCounterSensor extends Sensor {

    private int currentCount;
    private int maxCapacity;
    private boolean emergencyNotified;

    public EntryCounterSensor(String id, String location, IHardwareComm hardwareComm,
                              int maxCapacity) {
        super(id, location, hardwareComm);
        this.maxCapacity = maxCapacity;
        this.currentCount = 0;
        this.emergencyNotified = false;
    }

    /**
     * Lee el conteo desde el hardware. Si el Arduino reporta un numero mayor
     * al actual, dispara ENTRY_REGISTERED por cada nueva entrada detectada.
     */
    @Override
    public int readValue() {
        int hardwareCount = hardwareComm.readInt(id);
        if (hardwareCount > currentCount) {
            for (int i = currentCount + 1; i <= hardwareCount; i++) {
                currentCount = i;
                notifyObservers("ENTRY_REGISTERED", new Object[]{id, location, currentCount});
            }
            evaluateThreshold();
        }
        return currentCount;
    }

    /**
     * Registra una entrada individual al estadio.
     * Si se alcanza la capacidad máxima, notifica a los observadores una sola vez.
     */
    public void registerEntry() {
        currentCount++;
        // Notificar a persistencia con sensor_id, location y conteo actual
        notifyObservers("ENTRY_REGISTERED", new Object[]{id, location, currentCount});
        evaluateThreshold();
    }

    private void evaluateThreshold() {
        if (!emergencyNotified && currentCount >= maxCapacity) {
            emergencyNotified = true;
            notifyObservers("CAPACITY_EXCEEDED", currentCount);
        }
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    @Override
    public String getStatusLine() {
        return String.format("  Entradas:    %d / %d", currentCount, maxCapacity);
    }
}
