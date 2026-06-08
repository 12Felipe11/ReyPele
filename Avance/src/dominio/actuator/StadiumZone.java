package dominio.actuator;

/** Zona de iluminacion con soporte de color RGB (0-255). */
public class StadiumZone extends LightingZoneActuator {

    private int r, g, b;

    public StadiumZone(String id, String name) {
        super(id, name);
        this.r = 255;
        this.g = 255;
        this.b = 255;
    }

    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }

    public void setColor(int r, int g, int b) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
    }

    public String getColorHex() {
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
