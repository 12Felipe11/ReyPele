package dominio;

import dominio.actuator.AlarmActuator;
import dominio.actuator.LightingZoneActuator;
import dominio.actuator.StadiumZone;
import dominio.persistence.ISensorRepository;
import dominio.sensor.DistanceSensor;
import dominio.sensor.EntryCounterSensor;
import dominio.strategy.IStadiumModeStrategy;
import dominio.strategy.ManualModeStrategy;
import infraestructura.IHardwareComm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fachada del sistema de estadio.
 * Coordina sensores HC-SR04, actuadores, zonas de iluminacion y modos.
 */
public class StadiumFacade {

    private static final String[] ZONE_NAMES = {"NORTE", "SUR", "ORIENTAL", "OCCIDENTAL", "CANCHA"};

    private final IHardwareComm hardware;
    private final ISensorRepository repository;
    private final EntryCounterSensor entrySensor;
    private final DistanceSensor distanceSensor;
    private final LightingZoneActuator lightingActuator;
    private final AlarmActuator alarmActuator;
    private final StadiumConfig config;
    private final Map<String, StadiumZone> zones;
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

        this.zones = new LinkedHashMap<>();
        zones.put("NORTE",      new StadiumZone("LUZ-N", "Norte"));
        zones.put("SUR",        new StadiumZone("LUZ-S", "Sur"));
        zones.put("ORIENTAL",   new StadiumZone("LUZ-E", "Oriental"));
        zones.put("OCCIDENTAL", new StadiumZone("LUZ-O", "Occidental"));
        zones.put("CANCHA",     new StadiumZone("LUZ-C", "Cancha"));
    }

    /** HU-01: Lee sensores del Arduino y actualiza dominio. */
    public synchronized SensorData readAllSensors() {
        SensorData data = hardware.readSensors();
        int count = data.getEntryCount();
        boolean newEntry = false;
        if (lastEntryCount >= 0 && count > lastEntryCount) {
            int delta = count - lastEntryCount;
            for (int i = 1; i <= delta; i++) {
                System.out.printf("  [ENTRADA] Nueva entrada registrada. Total: %d%n",
                        lastEntryCount + i);
            }
            newEntry = true;
        } else if (lastEntryCount < 0 && count > 0) {
            newEntry = true;
        }
        lastEntryCount = count;
        entrySensor.updateValue(count);
        distanceSensor.updateValue(data.getDistanceCm(), data.isPresenceDetected());
        lightingActuator.setIntensity(data.getLightIntensity());
        if (newEntry) {
            repository.saveReading(data);
            repository.recordDailyEntry(count);
            repository.recordIndividualEntry(data.getDistanceCm());
        }
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
            if (on) {
                int count     = entrySensor.getCount();
                int threshold = config.getOccupancyThreshold();
                String reason = currentMode.getModeName().equals("EMERGENCIA")
                    ? "Modo emergencia activado"
                    : count > threshold
                        ? "Aforo máximo: " + count + "/" + threshold + " personas"
                        : "Activación manual";
                repository.recordDailyAlarm();
                repository.openAlarmHistory(reason);
                repository.logAudit("ALARM_ON", reason);
            } else {
                repository.closeAlarmHistory();
                repository.logAudit("ALARM_OFF", "Alarma desactivada");
            }
        }
        return ok;
    }

    /** Controla todas las zonas al mismo tiempo (luz global). */
    public synchronized boolean setLight(int intensity) {
        boolean ok = hardware.setLight(intensity);
        if (ok) {
            lightingActuator.setIntensity(intensity);
            lightingActuator.setActive(intensity > 0);
            for (StadiumZone z : zones.values()) {
                z.setIntensity(intensity);
                z.setActive(intensity > 0);
            }
            repository.saveEvent("LIGHT", "{\"intensity\":" + intensity + "}");
            repository.logAudit("LIGHT_CHANGE", "Global → " + intensity + "%");
            repository.recordLightHistory(intensity, "ALL");
        }
        return ok;
    }

    /** Controla la intensidad de una zona especifica. */
    public synchronized boolean setZoneLight(String zoneName, int intensity) {
        String key = zoneName.toUpperCase();
        StadiumZone zone = zones.get(key);
        if (zone == null) return false;
        hardware.setZoneLight(key, intensity);
        zone.setIntensity(intensity);
        zone.setActive(intensity > 0);
        repository.saveEvent("ZONE_LIGHT",
                "{\"zone\":\"" + key + "\",\"intensity\":" + intensity + "}");
        repository.recordLightHistory(intensity, key);
        return true;
    }

    /** Controla el color RGB de una zona. */
    public synchronized boolean setZoneColor(String zoneName, int r, int g, int b) {
        String key = zoneName.toUpperCase();
        StadiumZone zone = zones.get(key);
        if (zone == null) return false;
        hardware.setZoneColor(key, r, g, b);    // best-effort al hardware
        zone.setColor(r, g, b);
        return true;
    }

    /** Aplica intensidad a todas las zonas. */
    public synchronized void setAllZones(int intensity) {
        for (String key : new ArrayList<>(zones.keySet())) setZoneLight(key, intensity);
        lightingActuator.setIntensity(intensity);
        lightingActuator.setActive(intensity > 0);
        hardware.setLight(intensity);
    }

    /** Aplica color RGB a todas las zonas. */
    public synchronized void setAllZonesColor(int r, int g, int b) {
        for (String key : new ArrayList<>(zones.keySet())) setZoneColor(key, r, g, b);
    }

    public synchronized void setOccupancyThreshold(int val) {
        int before = config.getOccupancyThreshold();
        config.setOccupancyThreshold(val);
        if (config.getOccupancyThreshold() != before) {
            repository.updateConfig(config.getOccupancyThreshold(), config.getDistanceThreshold());
            repository.saveConfigHistory(config.getOccupancyThreshold(), config.getDistanceThreshold());
            repository.logAudit("CONFIG_CHANGE",
                "Umbral ocupación: " + before + " → " + config.getOccupancyThreshold() + " personas");
        }
    }

    public synchronized boolean setDistanceThreshold(float cm) {
        float before = config.getDistanceThreshold();
        boolean ok = hardware.setThreshold(cm);
        if (ok) {
            config.setDistanceThreshold(cm);
            repository.updateConfig(config.getOccupancyThreshold(), config.getDistanceThreshold());
            repository.saveConfigHistory(config.getOccupancyThreshold(), config.getDistanceThreshold());
            repository.logAudit("CONFIG_CHANGE",
                "Umbral distancia: " + before + " → " + cm + " cm");
        }
        return ok;
    }

    public synchronized void changeMode(IStadiumModeStrategy newMode) {
        String prevName = this.currentMode.getModeName();
        String newName  = newMode.getModeName();
        this.currentMode = newMode;
        repository.saveEvent("MODE", "{\"name\":\"" + newName + "\"}");
        repository.recordModeHistory(newName);
        repository.logAudit("MODE_CHANGE", prevName + " → " + newName);
    }

    /** Devuelve JSON de analítica para el dashboard web. */
    public String getDashboardAnalytics() {
        return repository.getDashboardAnalytics(
            entrySensor.getCount(), config.getOccupancyThreshold());
    }

    /** Devuelve JSON histórico agrupado por tipo y período. */
    public String getHistory(String type, String period) {
        return repository.getHistory(type, period);
    }

    /** Devuelve JSON con el historial completo de auditoría (paginado). */
    public String getAuditLog(String filter, int limit, int offset) {
        return repository.getAuditLog(filter, limit, offset);
    }

    /** Diagnóstico: conteos de cada tabla para verificar datos. */
    public String getDebugInfo() {
        return repository.getDebugInfo();
    }

    public EntryCounterSensor    getEntrySensor()      { return entrySensor; }
    public DistanceSensor        getDistanceSensor()    { return distanceSensor; }
    public LightingZoneActuator  getLightingActuator()  { return lightingActuator; }
    public AlarmActuator         getAlarmActuator()     { return alarmActuator; }
    public StadiumConfig         getConfig()            { return config; }
    public IStadiumModeStrategy  getCurrentMode()       { return currentMode; }
    public IHardwareComm         getHardware()          { return hardware; }
    public Map<String, StadiumZone> getZones()          { return Collections.unmodifiableMap(zones); }
}
