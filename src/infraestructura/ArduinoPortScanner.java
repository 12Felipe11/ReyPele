package infraestructura;

import com.fazecast.jSerialComm.SerialPort;

/**
 * Escanea los puertos serie disponibles y detecta automaticamente
 * el Arduino enviando READ y esperando una respuesta con "DATA:".
 * Prioriza puertos cuyo nombre/descripcion sugiere Arduino / USB-Serial.
 */
public final class ArduinoPortScanner {

    private static final int BAUD_RATE = 9600;
    private static final int OPEN_TIMEOUT_MS = 1500;
    private static final long RESET_DELAY_MS = 1500;

    private ArduinoPortScanner() {}

    /** Devuelve el nombre del puerto donde respondio el Arduino, o null. */
    public static String detect() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports == null || ports.length == 0) {
            System.out.println("  [ARDUINO] No se detectaron puertos serie.");
            return null;
        }

        System.out.println("  [ARDUINO] Buscando Arduino en puertos disponibles...");
        SerialPort[] ordered = prioritize(ports);

        for (SerialPort p : ordered) {
            String name = p.getSystemPortName();
            String desc = safe(p.getDescriptivePortName());
            System.out.println("    - probando " + name + "  (" + desc + ")");
            if (probe(p)) {
                System.out.println("  [ARDUINO] Detectado en " + name);
                return name;
            }
        }
        System.out.println("  [ARDUINO] No se encontro un Arduino que responda READ.");
        return null;
    }

    private static SerialPort[] prioritize(SerialPort[] ports) {
        SerialPort[] out = ports.clone();
        java.util.Arrays.sort(out, (a, b) -> score(b) - score(a));
        return out;
    }

    private static int score(SerialPort p) {
        String name = (p.getSystemPortName() + " " + safe(p.getDescriptivePortName())
                + " " + safe(p.getPortDescription())).toLowerCase();
        int s = 0;
        if (name.contains("arduino"))  s += 100;
        if (name.contains("ch340"))    s += 80;
        if (name.contains("ch9102"))   s += 80;
        if (name.contains("wchusb"))   s += 70;
        if (name.contains("usb-serial")) s += 60;
        if (name.contains("usb serial")) s += 60;
        if (name.contains("ftdi"))     s += 60;
        if (name.contains("silicon labs")) s += 60;
        if (name.contains("ttyacm"))   s += 50;
        if (name.contains("ttyusb"))   s += 40;
        if (name.matches(".*com\\d+.*")) s += 10;
        return s;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static boolean probe(SerialPort port) {
        try {
            port.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, OPEN_TIMEOUT_MS, 0);
            if (!port.openPort()) return false;

            try {
                Thread.sleep(RESET_DELAY_MS);
                drain(port);

                byte[] cmd = "READ\n".getBytes();
                port.writeBytes(cmd, cmd.length);

                long deadline = System.currentTimeMillis() + 2500;
                StringBuilder sb = new StringBuilder();
                byte[] one = new byte[1];
                while (System.currentTimeMillis() < deadline) {
                    int n = port.readBytes(one, 1);
                    if (n <= 0) continue;
                    char c = (char) (one[0] & 0xFF);
                    if (c == '\n') {
                        String line = sb.toString().trim();
                        sb.setLength(0);
                        if (line.startsWith("DATA:") || line.startsWith("READY")) return true;
                    } else if (c != '\r') {
                        sb.append(c);
                        if (sb.length() > 256) sb.setLength(0);
                    }
                }
                return false;
            } finally {
                port.closePort();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void drain(SerialPort port) {
        int avail = port.bytesAvailable();
        if (avail > 0) port.readBytes(new byte[avail], avail);
    }
}
