package infraestructura;

/**
 * Simula el hardware en consola sin necesidad de Arduino físico.
 * Devuelve valores fijos representativos para cada sensor.
 */
public class SimulatorAdapter implements IHardwareComm {

    // Simula que no hay presencia por defecto
    private boolean simulatedPresence = false;

    @Override
    public int readInt(String sensorId) {
        if (sensorId.startsWith("PRESENCE")) {
            return simulatedPresence ? 1 : 0;
        }
        // Para entradas: el simulador devuelve 0 (las entradas se registran manualmente)
        return 0;
    }

    @Override
    public boolean sendCommand(String deviceId, String command) {
        System.out.println("  [HW-SIM] Comando -> " + deviceId + ": " + command);
        return true;
    }

    /** Permite alternar la presencia simulada desde tests o la consola. */
    public void setSimulatedPresence(boolean presence) {
        this.simulatedPresence = presence;
    }
}
