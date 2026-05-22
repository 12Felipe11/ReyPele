package dominio.factory;

import infraestructura.ArduinoSerialAdapter;
import infraestructura.IHardwareComm;

public class RealHardwareFactory extends DeviceFactory {

    private final String portName;

    public RealHardwareFactory(String portName) {
        this.portName = portName;
    }

    @Override
    public IHardwareComm createHardwareComm() {
        return new ArduinoSerialAdapter(portName);
    }
}
