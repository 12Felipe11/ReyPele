package infraestructura;

import com.fazecast.jSerialComm.SerialPort;
import dominio.SensorData;

/**
 * Comunicacion serial con Arduino real via jSerialComm (HU-13).
 * Funciona en Windows (COMx), Linux (/dev/ttyACM*, /dev/ttyUSB*) y macOS.
 * Protocolo: READ -> DATA:count,dist,pres,luz
 */
public class ArduinoSerialAdapter implements IHardwareComm {

    private static final int  BAUD_RATE        = 9600;
    private static final int  READ_TIMEOUT_MS  = 3000;
    private static final long READY_WAIT_MS    = 8000;
    private static final int  READ_RETRIES     = 4;

    private final String portName;
    private SerialPort port;
    private boolean connected;

    public ArduinoSerialAdapter(String portName) {
        this.portName = portName;
    }

    @Override
    public void connect() {
        try {
            port = SerialPort.getCommPort(portName);
            port.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

            if (!port.openPort()) {
                System.err.println("  [ARDUINO] No se pudo abrir " + portName);
                connected = false;
                return;
            }

            System.out.println("  [ARDUINO] Esperando READY del Arduino...");
            long deadline = System.currentTimeMillis() + READY_WAIT_MS;
            boolean ready = false;
            while (System.currentTimeMillis() < deadline) {
                String line = readLine().trim();
                if ("READY".equals(line)) { ready = true; break; }
            }
            if (!ready) {
                System.err.println("  [ARDUINO] No llego READY en "
                        + READY_WAIT_MS + " ms, continuando igual.");
            }

            drain();
            connected = true;
            System.out.println("  [ARDUINO] Conectado en " + portName);
        } catch (Exception e) {
            System.err.println("  [ARDUINO] Error: " + e.getMessage());
            connected = false;
        }
    }

    @Override
    public void disconnect() {
        if (port != null && port.isOpen()) port.closePort();
        connected = false;
        System.out.println("  [ARDUINO] Desconectado.");
    }

    @Override
    public boolean isConnected() {
        return connected && port != null && port.isOpen();
    }

    private void drain() {
        if (port == null) return;
        int avail = port.bytesAvailable();
        if (avail > 0) port.readBytes(new byte[avail], avail);
    }

    private String readLine() {
        StringBuilder sb = new StringBuilder();
        long deadline = System.currentTimeMillis() + READ_TIMEOUT_MS;
        byte[] one = new byte[1];
        while (System.currentTimeMillis() < deadline) {
            int n = port.readBytes(one, 1);
            if (n <= 0) continue;
            char c = (char) (one[0] & 0xFF);
            if (c == '\n') break;
            if (c != '\r') sb.append(c);
        }
        return sb.toString();
    }

    private synchronized String sendCommand(String command) {
        if (!isConnected()) return "ERROR:NOT_CONNECTED";
        try {
            drain();
            byte[] bytes = (command + "\n").getBytes();
            port.writeBytes(bytes, bytes.length);
            String line = readLine();
            if (line.isEmpty()) return "ERROR:TIMEOUT";
            return line.trim();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    @Override
    public SensorData readSensors() {
        String resp = sendCommand("READ");
        // El Arduino tambien publica DATA cada 500 ms por su cuenta, asi
        // que puede llegar ruido (READY de arranque, ENTRY:n, fragmentos).
        // Reintentamos leyendo lineas hasta encontrar una DATA:* valida.
        for (int i = 0; i < READ_RETRIES; i++) {
            if (resp.startsWith("DATA:")) return SensorData.parse(resp);
            resp = readLine().trim();
            if (resp.isEmpty()) break;
        }
        System.err.println("  [ARDUINO] Sin respuesta DATA valida tras "
                + READ_RETRIES + " intentos.");
        return new SensorData(0, 999.0f, false, 0);
    }

    @Override
    public boolean setAlarm(boolean on) {
        String resp = sendCommand(on ? "ALARM_ON" : "ALARM_OFF");
        return !resp.startsWith("ERROR");
    }

    @Override
    public boolean setLight(int intensity) {
        String resp = sendCommand("LIGHT_SET:" + Math.max(0, Math.min(100, intensity)));
        return !resp.startsWith("ERROR");
    }

    @Override
    public boolean setThreshold(float cm) {
        String resp = sendCommand("SET_THRESHOLD:" + cm);
        return !resp.startsWith("ERROR");
    }
}
