package dominio;

import dominio.actuator.Actuator;
import dominio.sensor.Sensor;
import dominio.strategy.IStadiumModeStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fachada del sistema de estadio.
 * Centraliza el acceso a sensores, actuadores y el modo actual,
 * simplificando la interacción para las capas superiores.
 */
public class StadiumFacade {

    private List<Sensor> sensors;
    private List<Actuator> actuators;
    private IStadiumModeStrategy currentMode;

    public StadiumFacade() {
        this.sensors = new ArrayList<>();
        this.actuators = new ArrayList<>();
    }

    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
    }

    public void addActuator(Actuator actuator) {
        actuators.add(actuator);
    }

    /**
     * Cambia el modo del estadio y ejecuta la estrategia asociada.
     */
    public void changeMode(IStadiumModeStrategy newMode) {
        this.currentMode = newMode;
        currentMode.execute(this);
    }

    public List<Sensor> getSensors() {
        return Collections.unmodifiableList(sensors);
    }

    public List<Actuator> getActuators() {
        return Collections.unmodifiableList(actuators);
    }

    public IStadiumModeStrategy getCurrentMode() {
        return currentMode;
    }
}
