package dominio.sensor;

import dominio.Observer;
import infraestructura.IHardwareComm;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase abstracta base para todos los sensores del estadio.
 * Implementa el lado "Subject" del patrón Observer.
 */
public abstract class Sensor {

    protected String id;
    protected String location;
    protected IHardwareComm hardwareComm;
    private List<Observer> observers = new ArrayList<>();

    public Sensor(String id, String location, IHardwareComm hardwareComm) {
        this.id = id;
        this.location = location;
        this.hardwareComm = hardwareComm;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    protected void notifyObservers(String eventType, Object data) {
        for (Observer observer : observers) {
            observer.update(eventType, data);
        }
    }

    public abstract int readValue();

    /** Descripcion legible del estado actual para el comando READ. */
    public abstract String getStatusLine();

    public String getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }
}
