package dominio.factory;

import dominio.sensor.EntryCounterSensor;
import dominio.actuator.AlarmZoneActuator;
import dominio.actuator.LightingZoneActuator;
import infraestructura.ArduinoAdapter;
import infraestructura.IHardwareComm;

/**
 * Crea dispositivos que se comunican con hardware físico.
 */
public class RealHardwareFactory extends DeviceFactory {

    private final IHardwareComm comm;

    public RealHardwareFactory() {
        this.comm = new ArduinoAdapter();
    }

    @Override
    public EntryCounterSensor createEntryCounterSensor(String id, String location,
                                                        int maxCapacity) {
        return new EntryCounterSensor(id, location, comm, maxCapacity);
    }

    @Override
    public AlarmZoneActuator createAlarmZoneActuator(String id, String zone) {
        return new AlarmZoneActuator(id, zone, comm);
    }

    @Override
    public LightingZoneActuator createLightingZoneActuator(String id, String zone) {
        return new LightingZoneActuator(id, zone, comm);
    }
}
