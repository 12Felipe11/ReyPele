package infraestructura;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Comunicacion real con Arduino via puerto serial (jSerialComm).
 *
 * Protocolo recibido desde Arduino:
 *   DATA:count,distance,presence,lightIntensity   (cada 500ms)
 *   ENTRY:totalCount                               (por cada entrada detectada)
 *
 * Comandos enviados al Arduino:
 *   ALARM_ON / ALARM_OFF
 *   LIGHT_SET:0-100
 *   SET_THRESHOLD:cm
 *   READ
 *
 * Dependencia: jSerialComm-2.x.x.jar en el classpath del proyecto.
 * Descargar: https://fazecast.github.io/jSerialComm/
 */
public class ArduinoAdapter implements IHardwareComm {

    // ---- Configuracion de conexion ----
    // Ajusta COM3 al puerto que aparece en Arduino IDE (Herramientas > Puerto)
    private static final String PORT_NAME  = "COM3";
    private static final int    BAUD_RATE  = 9600;
    private static final int    READ_TIMEOUT_MS = 1000;

    // IDs de sensor reconocidos por readInt()
    public static final String SENSOR_ENTRY    = "ENTRY-01";
    public static final String SENSOR_DISTANCE = "DISTANCE";
    public static final String SENSOR_PRESENCE = "PRESENCE";
    public static final String SENSOR_LIGHT    = "LIGHT-SENSOR";

    private SerialPort     serialPort;
    private BufferedReader reader;
    private PrintWriter    writer;

    // Ultimo estado leido del Arduino
    private volatile int     lastEntryCount   = 0;
    private volatile float   lastDistance     = 999.0f;
    private volatile boolean lastPresence     = false;
    private volatile int     lastLightPercent = 0;
    private volatile boolean connected        = false;

    public ArduinoAdapter() {
        connect();
        if (connected) {
            startReaderThread();
        }
    }

    // ---------------------------------------------------------------
    // Conexion
    // ---------------------------------------------------------------
    private void connect() {
        serialPort = SerialPort.getCommPort(PORT_NAME);
        serialPort.setComPortParameters(BAUD_RATE, 8, 1, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

        if (serialPort.openPort()) {
            reader    = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            writer    = new PrintWriter(serialPort.getOutputStream(), true);
            connected = true;
            System.out.println("[Arduino] Conectado en " + PORT_NAME + " @ " + BAUD_RATE + " baud");
            // Esperar que Arduino termine su setup()
            try { Thread.sleep(2000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            System.err.println("[Arduino] ERROR: no se pudo abrir " + PORT_NAME
                    + ". Verifica el puerto y que no este en uso por Arduino IDE.");
        }
    }

    // ---------------------------------------------------------------
    // Hilo de lectura continua en segundo plano
    // ---------------------------------------------------------------
    private void startReaderThread() {
        Thread t = new Thread(() -> {
            while (connected && serialPort.isOpen()) {
                try {
                    String line = reader.readLine();
                    if (line != null) parseLine(line.trim());
                } catch (Exception e) {
                    if (connected) {
                        System.err.println("[Arduino] Error de lectura: " + e.getMessage());
                    }
                }
            }
        }, "arduino-reader");
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------
    // Parseo del protocolo Arduino
    // ---------------------------------------------------------------
    private void parseLine(String line) {
        if (line.startsWith("DATA:")) {
            // DATA:count,distance,presence,lightIntensity
            String[] parts = line.substring(5).split(",");
            if (parts.length >= 4) {
                try {
                    lastEntryCount   = Integer.parseInt(parts[0].trim());
                    lastDistance     = Float.parseFloat(parts[1].trim());
                    lastPresence     = parts[2].trim().equals("1");
                    lastLightPercent = Integer.parseInt(parts[3].trim());
                } catch (NumberFormatException ignored) {}
            }
        } else if (line.startsWith("ENTRY:")) {
            // ENTRY:totalCount  →  entrada puntual detectada
            try {
                lastEntryCount = Integer.parseInt(line.substring(6).trim());
                System.out.println("[Arduino] Nueva entrada detectada. Total: " + lastEntryCount);
            } catch (NumberFormatException ignored) {}
        }
    }

    // ---------------------------------------------------------------
    // IHardwareComm
    // ---------------------------------------------------------------
    @Override
    public int readInt(String sensorId) {
        if (!connected) return 0;
        switch (sensorId) {
            case SENSOR_ENTRY:    return lastEntryCount;
            case SENSOR_DISTANCE: return (int) lastDistance;
            case SENSOR_PRESENCE:
            case "PRESENCE-01":   return lastPresence ? 1 : 0;
            case SENSOR_LIGHT:    return lastLightPercent;
            default:              return 0;
        }
    }

    @Override
    public boolean sendCommand(String deviceId, String command) {
        if (!connected || writer == null) {
            System.err.println("[Arduino] Comando ignorado (sin conexion): " + command);
            return false;
        }
        writer.println(command);
        System.out.println("[Arduino] Comando enviado -> " + command);
        return true;
    }

    // ---------------------------------------------------------------
    // Utilidad
    // ---------------------------------------------------------------
    /** Solicita una lectura inmediata al Arduino. */
    public void requestRead() {
        sendCommand("", "READ");
    }

    /** Cierra la conexion serial limpiamente. Llamar al cerrar la app. */
    public void disconnect() {
        connected = false;
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            System.out.println("[Arduino] Desconectado.");
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
