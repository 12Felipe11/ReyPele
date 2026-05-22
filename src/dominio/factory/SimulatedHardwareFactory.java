package dominio.factory;

import infraestructura.IHardwareComm;
import infraestructura.SimulatorAdapter;

public class SimulatedHardwareFactory extends DeviceFactory {
    @Override
    public IHardwareComm createHardwareComm() {
        return new SimulatorAdapter();
    }
}
