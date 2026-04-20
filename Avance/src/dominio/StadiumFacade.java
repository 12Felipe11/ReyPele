package dominio;

import dominio.actuator.AlarmActuator;
import dominio.actuator.LightingZoneActuator;
import dominio.persistence.ISensorRepository;
import dominio.sensor.DistanceSensor;
import dominio.sensor.EntryCounterSensor;
import dominio.strategy.IStadiumModeStrategy;
import dominio.strategy.ManualModeStrategy;
import infraestructura.IHardwareComm;

import java.util.List;

/**
 * Fachada del sistema de estadio.
 * Coordina sensores HC-SR04, actuadores y modos de operacion.
 */
public class StadiumFacade {

    private final IHardwareComm hardware;
    private final ISensorRepository repository;
    private final EntryCounterSensor entrySensor;
    private final DistanceSensor distanceSensor;
    private final LightingZoneActuator lightingActuator;
    private final AlarmActuator alarmActuator;
    private final StadiumConfig config;
    private IStadiumModeStrategy currentMode;
    private int lastEntryCount = -1;

    public StadiumFacade(IHardwareComm hardware) {
        this(hardware, new infraestructura.NoOpSensorRepository());
    }

    public StadiumFacade(IHardwareComm hardware, ISensorRepository repository) {
        this.hardware = hardware;
        this.repository = repository;
        this.entrySensor = new EntryCounterSensor("ENTRY-01", "Puerta Norte");
        this.distanceSensor = new DistanceSensor("HC-SR04", "Puerta Norte");
        this.lightingActuator = new LightingZoneActuator("LUZ-01", "ZONE_A");
        this.alarmActuator = new AlarmActuator("ALARM-01", "General");
        this.config = new StadiumConfig();
        this.currentMode = new ManualModeStrategy();
    }

    /** HU-01: Lee sensores del Arduino y actualiza dominio. */
    public synchronized SensorData readAllSensors() {
        SensorData data = hardware.readSensors();
        int count = data.getEntryCount();
        if (lastEntryCount >= 0 && count > lastEntryCount) {
            int delta = count - lastEntryCount;
            for (int i = 1; i <= delta; i++) {
                System.out.printf("  [ENTRADA] Nueva entrada registrada. Total: %d%n",
                        lastEntryCount + i);
            }
        }
        lastEntryCount = count;
        entrySensor.updateValue(count);
        distanceSensor.updateValue(data.getDistanceCm(), data.isPresenceDetected());
        lightingActuator.setIntensity(data.getLightIntensity());
        repository.saveReading(data);
        return data;
    }

    public synchronized List<String> evaluateRules() {
        return currentMode.evaluate(this);
    }

    public synchronized boolean setAlarm(boolean on) {
        boolean ok = hardware.setAlarm(on);
        if (ok) {
            alarmActuator.setActive(on);
            repository.saveEvent("ALARM", "{\"on\":" + on + "}");
        }
        return ok;
    }

    public synchronized boolean setLight(int intensity) {
        boolean ok = hardware.setLight(intensity);
        if (ok) {
            lightingActuator.setIntensity(intensity);
            lightingActuator.setActive(intensity > 0);
            repository.saveEvent("LIGHT", "{\"intensity\":" + intensity + "}");
        }
        return ok;
    }

    public synchronized void setOccupancyThreshold(int val) {
        int before = config.getOccupancyThreshold();
        config.setOccupancyThreshold(val);
        if (config.getOccupancyThreshold() != before) {
            repository.updateConfig(config.getOccupancyThreshold(), config.getDistanceThreshold());
        }
    }

    public synchronized boolean setDistanceThreshold(float cm) {
        boolean ok = hardware.setThreshold(cm);
        if (ok) {
            config.setDistanceThreshold(cm);
            repository.updateConfig(config.getOccupancyThreshold(), config.getDistanceThreshold());
        }
        return ok;
    }

    public synchronized void changeMode(IStadiumModeStrategy newMode) {
        this.currentMode = newMode;
        repository.saveEvent("MODE", "{\"name\":\"" + newMode.getModeName() + "\"}");
    }

    public EntryCounterSensor    getEntrySensor()      { return entrySensor; }
    public DistanceSensor        getDistanceSensor()    { return distanceSensor; }
    public LightingZoneActuator  getLightingActuator()  { return lightingActuator; }
    public AlarmActuator         getAlarmActuator()     { return alarmActuator; }
    public StadiumConfig         getConfig()            { return config; }
    public IStadiumModeStrategy  getCurrentMode()       { return currentMode; }
    public IHardwareComm         getHardware()          { return hardware; }
}
