package dominio.actuator;

public abstract class Actuator {

    protected String id;
    protected String zone;
    protected boolean active;

    public Actuator(String id, String zone) {
        this.id = id;
        this.zone = zone;
        this.active = false;
    }

    public String getId()     { return id; }
    public String getZone()   { return zone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
