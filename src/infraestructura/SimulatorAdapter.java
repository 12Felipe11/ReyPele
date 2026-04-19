package infraestructura;

/**
 * Imprime las operaciones en consola para simulaciones
 */
public class SimulatorAdapter implements IHardwareComm {

    @Override
    public int readInt(String sensorId) {
        System.out.println("  [HW-SIM] Lectura de sensor " + sensorId);
        return 0;
    }

    @Override
    public boolean sendCommand(String deviceId, String command) {
        System.out.println("  [HW-SIM] Comando -> " + deviceId + ": " + command);
        return true;
    }
}
