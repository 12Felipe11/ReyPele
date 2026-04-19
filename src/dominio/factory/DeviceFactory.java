package dominio.factory;

import dominio.sensor.EntryCounterSensor;
import dominio.actuator.AlarmZoneActuator;
import dominio.actuator.LightingZoneActuator;

/**
 * Define la interfaz para crear familias de sensores y actuadores,
 * sin acoplarse a una implementación de hardware específica.
 */
public abstract class DeviceFactory {

    public abstract EntryCounterSensor createEntryCounterSensor(
            String id, String location, int maxCapacity);

    public abstract AlarmZoneActuator createAlarmZoneActuator(
            String id, String zone);

    public abstract LightingZoneActuator createLightingZoneActuator(
            String id, String zone);
}
