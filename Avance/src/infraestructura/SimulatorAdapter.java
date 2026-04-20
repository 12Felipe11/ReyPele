package infraestructura;

import dominio.SensorData;
import java.util.Random;

/**
 * Simulador del HC-SR04 + actuadores (HU-14).
 * Genera lecturas de distancia y conteo por flanco.
 */
public class SimulatorAdapter implements IHardwareComm {

    private boolean connected;
    private int entryCount;
    private float distanceCm;
    private boolean lastPresence;
    private int lightIntensity;
    private float threshold;
    private final Random random;

    public SimulatorAdapter() {
        this.random = new Random();
        this.entryCount = 0;
        this.distanceCm = 999.0f;
        this.lastPresence = false;
        this.lightIntensity = 0;
        this.threshold = 30.0f;
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
        // Simular distancia: 60% lejos, 40% cerca
        distanceCm = random.nextInt(100) < 40
                ? 5 + random.nextFloat() * (threshold - 5)   // cerca (presencia)
                : threshold + random.nextFloat() * 200;       // lejos

        boolean presence = distanceCm < threshold;

        // HU-03: contar solo en flanco (acerca + aleja)
        if (lastPresence && !presence) {
            entryCount++;
        }
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
        System.out.println("  [SIM-HW] Luces: " + lightIntensity + "%");
        return true;
    }

    @Override
    public boolean setThreshold(float cm) {
        threshold = cm;
        System.out.println("  [SIM-HW] Umbral distancia: " + cm + " cm");
        return true;
    }
}
