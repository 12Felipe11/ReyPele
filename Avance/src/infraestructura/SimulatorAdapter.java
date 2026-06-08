package infraestructura;

import dominio.SensorData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulador del HC-SR04 + actuadores (HU-14).
 * Genera lecturas de distancia y conteo por flanco.
 * Soporta control de zonas con intensidad y color RGB.
 */
public class SimulatorAdapter implements IHardwareComm {

    private boolean connected;
    private int entryCount;
    private float distanceCm;
    private boolean lastPresence;
    private int lightIntensity;
    private float threshold;
    private final Random random;

    // zone -> [intensity, r, g, b]
    private final Map<String, int[]> zoneStates = new LinkedHashMap<>();

    public SimulatorAdapter() {
        this.random = new Random();
        this.entryCount = 0;
        this.distanceCm = 999.0f;
        this.lastPresence = false;
        this.lightIntensity = 0;
        this.threshold = 30.0f;
        for (String z : new String[]{"NORTE", "SUR", "ORIENTAL", "OCCIDENTAL", "CANCHA"}) {
            zoneStates.put(z, new int[]{0, 255, 255, 255});
        }
    }

    @Override
    public void connect() {
        connected = true;
        System.out.println("  [SIMULADOR] Hardware simulado conectado.");
    }

    @Override
    public void disconnect() {
        connected = false;
        System.out.println("  [SIMULADOR] Desconectado.");
    }

    @Override
    public boolean isConnected() { return connected; }

    @Override
    public SensorData readSensors() {
        distanceCm = random.nextInt(100) < 40
                ? 5 + random.nextFloat() * (threshold - 5)
                : threshold + random.nextFloat() * 200;

        boolean presence = distanceCm < threshold;
        if (lastPresence && !presence) entryCount++;
        lastPresence = presence;

        return new SensorData(entryCount, distanceCm, presence, lightIntensity);
    }

    @Override
    public boolean setAlarm(boolean on) {
        System.out.println("  [SIM-HW] Alarma: " + (on ? "ACTIVADA" : "DESACTIVADA"));
        return true;
    }

    @Override
    public boolean setLight(int intensity) {
        lightIntensity = Math.max(0, Math.min(100, intensity));
        System.out.println("  [SIM-HW] Luces global: " + lightIntensity + "%");
        return true;
    }

    @Override
    public boolean setThreshold(float cm) {
        threshold = cm;
        System.out.println("  [SIM-HW] Umbral distancia: " + cm + " cm");
        return true;
    }

    @Override
    public boolean setZoneLight(String zone, int intensity) {
        int[] state = zoneStates.get(zone.toUpperCase());
        if (state == null) return false;
        state[0] = Math.max(0, Math.min(100, intensity));
        System.out.printf("  [SIM-HW] Zona %-10s intensidad: %3d%%%n", zone, state[0]);
        return true;
    }

    @Override
    public boolean setZoneColor(String zone, int r, int g, int b) {
        int[] state = zoneStates.get(zone.toUpperCase());
        if (state == null) return false;
        state[1] = Math.max(0, Math.min(255, r));
        state[2] = Math.max(0, Math.min(255, g));
        state[3] = Math.max(0, Math.min(255, b));
        System.out.printf("  [SIM-HW] Zona %-10s color: rgb(%d,%d,%d)%n", zone, r, g, b);
        return true;
    }
}
