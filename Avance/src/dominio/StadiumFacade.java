package dominio;

import dominio.actuator.AlarmActuator;
import dominio.actuator.LightingZoneActuator;
import dominio.event.EventType;
import dominio.event.IStadiumObserver;
import dominio.event.StadiumEvent;
import dominio.sensor.DistanceSensor;
import dominio.sensor.EntryCounterSensor;
import dominio.strategy.IStadiumModeStrategy;
import dominio.strategy.ManualModeStrategy;
import infraestructura.IHardwareComm;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fachada del sistema de estadio.
 * Coordina sensores HC-SR04, actuadores y modos de operacion.
 *
 * Actua tambien como Subject del patron Observer: emite eventos a los
 * observadores registrados cuando ocurren cambios relevantes (nueva entrada,
 * cambio de alarma/luces/modo/umbral).
 */
public class StadiumFacade {

    private final IHardwareComm hardware;
    private final EntryCounterSensor entrySensor;
    private final DistanceSensor distanceSensor;
    private final LightingZoneActuator lightingActuator;
    private final AlarmActuator alarmActuator;
    private final StadiumConfig config;
    private IStadiumModeStrategy currentMode;

    private final List<IStadiumObserver> observers = new CopyOnWriteArrayList<>();
    private int lastEntryCount = 0;

    public StadiumFacade(IHardwareComm hardware) {
        this.hardware = hardware;
        this.entrySensor = new EntryCounterSensor("ENTRY-01", "Puerta Norte");
        this.distanceSensor = new DistanceSensor("HC-SR04", "Puerta Norte");
        this.lightingActuator = new LightingZoneActuator("LUZ-01", "ZONE_A");
        this.alarmActuator = new AlarmActuator("ALARM-01", "General");
        this.config = new StadiumConfig();
        this.currentMode = new ManualModeStrategy();
    }

    public void addObserver(IStadiumObserver observer)    { observers.add(observer); }
    public void removeObserver(IStadiumObserver observer) { observers.remove(observer); }

    private void notifyObservers(EventType type, String description) {
        StadiumEvent event = new StadiumEvent(type, description);
        for (IStadiumObserver o : observers) {
            try { o.onEvent(event); }
            catch (Exception e) {
                System.err.println("  [OBSERVER] Error en " + o.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        }
    }

    /** HU-01: Lee sensores del Arduino y actualiza dominio. */
    public synchronized SensorData readAllSensors() {
        SensorData data = hardware.readSensors();
        int newCount = data.getEntryCount();

        entrySensor.updateValue(newCount);
        distanceSensor.updateValue(data.getDistanceCm(), data.isPresenceDetected());
        lightingActuator.setIntensity(data.getLightIntensity());

        // Detectar nuevas entradas comparando contra el ultimo conteo conocido
        if (newCount > lastEntryCount) {
            int delta = newCount - lastEntryCount;
            for (int i = 0; i < delta; i++) {
                notifyObservers(EventType.ENTRY_DETECTED,
                        "Nueva entrada (total: " + (lastEntryCount + i + 1) + ")");
            }
            lastEntryCount = newCount;
        }

        notifyObservers(EventType.SENSOR_READ,
                String.format("dist=%.1fcm pres=%s entradas=%d luz=%d%%",
                        data.getDistanceCm(),
                        data.isPresenceDetected() ? "SI" : "NO",
                        newCount,
                        data.getLightIntensity()));
        return data;
    }

    public synchronized List<String> evaluateRules() {
        return currentMode.evaluate(this);
    }

    public synchronized boolean setAlarm(boolean on) {
        boolean previous = alarmActuator.isActive();
        boolean ok = hardware.setAlarm(on);
        if (ok) {
            alarmActuator.setActive(on);
            if (previous != on) {
                notifyObservers(EventType.ALARM_CHANGED,
                        "Alarma " + (on ? "ACTIVADA" : "DESACTIVADA"));
            }
        }
        return ok;
    }

    public synchronized boolean setLight(int intensity) {
        int previous = lightingActuator.getIntensity();
        boolean ok = hardware.setLight(intensity);
        if (ok) {
            lightingActuator.setIntensity(intensity);
            lightingActuator.setActive(intensity > 0);
            if (previous != lightingActuator.getIntensity()) {
                notifyObservers(EventType.LIGHT_CHANGED,
                        "Luces " + previous + "% -> " + lightingActuator.getIntensity() + "%");
            }
        }
        return ok;
    }

    public synchronized boolean setDistanceThreshold(float cm) {
        float previous = config.getDistanceThreshold();
        boolean ok = hardware.setThreshold(cm);
        if (ok) {
            config.setDistanceThreshold(cm);
            notifyObservers(EventType.THRESHOLD_CHANGED,
                    String.format("Umbral distancia %.1fcm -> %.1fcm", previous, cm));
        }
        return ok;
    }

    public void changeMode(IStadiumModeStrategy newMode) {
        String previous = (this.currentMode != null) ? this.currentMode.getModeName() : "NONE";
        this.currentMode = newMode;
        notifyObservers(EventType.MODE_CHANGED,
                "Modo " + previous + " -> " + newMode.getModeName());
    }

    public EntryCounterSensor    getEntrySensor()      { return entrySensor; }
    public DistanceSensor        getDistanceSensor()    { return distanceSensor; }
    public LightingZoneActuator  getLightingActuator()  { return lightingActuator; }
    public AlarmActuator         getAlarmActuator()     { return alarmActuator; }
    public StadiumConfig         getConfig()            { return config; }
    public IStadiumModeStrategy  getCurrentMode()       { return currentMode; }
    public IHardwareComm         getHardware()          { return hardware; }
}
