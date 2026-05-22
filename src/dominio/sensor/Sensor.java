package dominio.sensor;

public abstract class Sensor {

    protected String id;
    protected String location;

    public Sensor(String id, String location) {
        this.id = id;
        this.location = location;
    }

    public String getId()       { return id; }
    public String getLocation() { return location; }
}
