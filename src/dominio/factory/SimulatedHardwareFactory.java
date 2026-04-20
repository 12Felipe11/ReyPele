package dominio.factory;

import dominio.sensor.EntryCounterSensor;
import dominio.sensor.PresenceSensor;
import dominio.actuator.AlarmZoneActuator;
import dominio.actuator.LightingZoneActuator;
import infraestructura.IHardwareComm;
import infraestructura.SimulatorAdapter;

/**
 * Crea dispositivos que usan SimulatorAdapter como capa de hardware.
 */
public class SimulatedHardwareFactory extends DeviceFactory {

    private final IHardwareComm comm;

    public SimulatedHardwareFactory() {
        this.comm = new SimulatorAdapter();
    }

    @Override
    public EntryCounterSensor createEntryCounterSensor(String id, String location,
                                                        int maxCapacity) {
        return new EntryCounterSensor(id, location, comm, maxCapacity);
    }

    @Override
    public PresenceSensor createPresenceSensor(String id, String location) {
        return new PresenceSensor(id, location, comm);
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
