package dominio.actuator;

import infraestructura.IHardwareComm;

/**
 * Clase abstracta base para todos los actuadores del estadio.
 * Define la interfaz común de activación/desactivación.
 */
public abstract class Actuator {

    protected String id;
    protected String zone;
    protected boolean active;
    protected IHardwareComm hardwareComm;

    public Actuator(String id, String zone, IHardwareComm hardwareComm) {
        this.id = id;
        this.zone = zone;
        this.hardwareComm = hardwareComm;
        this.active = false;
    }

    public abstract void activate();

    public abstract void deactivate();

    public String getId() {
        return id;
    }

    public String getZone() {
        return zone;
    }

    public boolean isActive() {
        return active;
    }
}
