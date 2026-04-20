package aplicacion;

import dominio.SensorData;
import dominio.StadiumFacade;
import dominio.strategy.AutoModeStrategy;
import dominio.strategy.EmergencyModeStrategy;
import dominio.strategy.ManualModeStrategy;

import java.util.List;

/**
 * Controlador de aplicacion.
 * Procesa todos los comandos del operador (HU-01 a HU-14).
 * Protocolo alineado con Arduino ReyPele (HC-SR04 + OLED SSD1306).
 */
public class StadiumController {

    private final StadiumFacade facade;

    public StadiumController(StadiumFacade facade) {
        this.facade = facade;
    }

    public String processCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "";

        switch (parts[0].toUpperCase()) {
            case "READ":   return handleRead();
            case "LIGHT":  return handleLight(parts);
            case "ALARM":  return handleAlarm(parts);
            case "MODE":   return handleMode(parts);
            case "SET":    return handleSet(parts);
            case "STATUS": return handleStatus();
            case "HELP":   return getHelp();
            case "EXIT":   return null;
            default:
                return "  Comando no reconocido. Escriba HELP.";
        }
    }

    /** HU-01 + HU-02 + HU-03 */
    private String handleRead() {
        SensorData data = facade.readAllSensors();
        StringBuilder sb = new StringBuilder();
        sb.append("  --- LECTURA DE SENSORES (HC-SR04) ---\n");
        sb.append(String.format("  Distancia:     %.1f cm (%s)%n",
                data.getDistanceCm(),
                facade.getDistanceSensor().getDistanceDescription()));
        sb.append(String.format("  Presencia:     %s%n",
                data.isPresenceDetected() ? "DETECTADA" : "Sin presencia"));
        sb.append(String.format("  Entradas:      %d / %d (umbral ocupacion)%n",
                data.getEntryCount(),
                facade.getConfig().getOccupancyThreshold()));
        sb.append(String.format("  Luces:         %d%%%n", data.getLightIntensity()));

        List<String> actions = facade.evaluateRules();
        if (!actions.isEmpty()) {
            sb.append("  --- ACCIONES AUTOMATICAS [")
              .append(facade.getCurrentMode().getModeName()).append("] ---\n");
            for (String a : actions) sb.append("  > ").append(a).append("\n");
        }
        return sb.toString();
    }

    /** HU-04: LIGHT <0-100> o LIGHT ON/OFF */
    private String handleLight(String[] parts) {
        if (parts.length < 2)
            return "  Uso: LIGHT <0-100> | LIGHT ON | LIGHT OFF";
        if (!facade.getCurrentMode().canControlLight())
            return "  No permitido en modo " + facade.getCurrentMode().getModeName();
        String arg = parts[1].toUpperCase();
        int intensity;
        if ("ON".equals(arg))       intensity = 100;
        else if ("OFF".equals(arg)) intensity = 0;
        else {
            try { intensity = Integer.parseInt(arg); }
            catch (NumberFormatException e) { return "  Valor invalido."; }
        }
        facade.setLight(intensity);
        return "  Luces: " + intensity + "%";
    }

    /** HU-06 */
    private String handleAlarm(String[] parts) {
        if (parts.length < 2) return "  Uso: ALARM ON|OFF";
        if (!facade.getCurrentMode().canControlAlarm())
            return "  No permitido en modo " + facade.getCurrentMode().getModeName();
        boolean on = "ON".equalsIgnoreCase(parts[1]);
        facade.setAlarm(on);
        return "  Alarma: " + (on ? "ACTIVADA" : "DESACTIVADA");
    }

    /** HU-08 */
    private String handleMode(String[] parts) {
        if (parts.length < 2)
            return "  Uso: MODE AUTO|MANUAL|EMERGENCY";
        String name = parts[1].toUpperCase();
        switch (name) {
            case "AUTO":      facade.changeMode(new AutoModeStrategy()); break;
            case "MANUAL":    facade.changeMode(new ManualModeStrategy()); break;
            case "EMERGENCY": facade.changeMode(new EmergencyModeStrategy()); break;
            default: return "  Modo invalido. Opciones: AUTO, MANUAL, EMERGENCY";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("  Modo cambiado a: ").append(name).append("\n");
        List<String> actions = facade.evaluateRules();
        for (String a : actions) sb.append("  > ").append(a).append("\n");
        return sb.toString();
    }

    /** HU-12: SET THRESHOLD <N> | SET DISTANCE <cm> */
    private String handleSet(String[] parts) {
        if (parts.length < 3)
            return "  Uso: SET THRESHOLD <personas> | SET DISTANCE <cm>";
        String type = parts[1].toUpperCase();
        try {
            if ("THRESHOLD".equals(type)) {
                int val = Integer.parseInt(parts[2]);
                if (val <= 0) return "  Debe ser positivo.";
                facade.setOccupancyThreshold(val);
                return "  Umbral de ocupacion: " + val + " personas";
            } else if ("DISTANCE".equals(type)) {
                float cm = Float.parseFloat(parts[2]);
                if (cm <= 0) return "  Debe ser positivo.";
                facade.setDistanceThreshold(cm);
                return "  Umbral de distancia: " + cm + " cm";
            }
        } catch (NumberFormatException e) {
            return "  Valor invalido.";
        }
        return "  Uso: SET THRESHOLD <N> | SET DISTANCE <cm>";
    }

    private String handleStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("  --- ESTADO DEL SISTEMA ---\n");
        sb.append(String.format("  Modo:            %s%n", facade.getCurrentMode().getModeName()));
        sb.append(String.format("  Umbral personas: %d%n", facade.getConfig().getOccupancyThreshold()));
        sb.append(String.format("  Umbral distancia:%.1f cm%n", facade.getConfig().getDistanceThreshold()));
        sb.append(String.format("  Alarma:          %s%n", facade.getAlarmActuator().isActive() ? "ACTIVA" : "INACTIVA"));
        sb.append(String.format("  Luces:           %d%%%n", facade.getLightingActuator().getIntensity()));
        sb.append(String.format("  Distancia:       %.1f cm%n", facade.getDistanceSensor().getDistanceCm()));
        sb.append(String.format("  Presencia:       %s%n", facade.getDistanceSensor().isPresenceDetected() ? "SI" : "NO"));
        sb.append(String.format("  Entradas:        %d%n", facade.getEntrySensor().getCount()));
        sb.append(String.format("  Hardware:        %s%n", facade.getHardware().isConnected() ? "CONECTADO" : "DESCONECTADO"));
        return sb.toString();
    }

    private String getHelp() {
        return "  --- COMANDOS (ReyPele HC-SR04) ---\n"
             + "  READ                       Leer sensores\n"
             + "  LIGHT <0-100|ON|OFF>       Control de luces\n"
             + "  ALARM ON|OFF               Control de alarma\n"
             + "  MODE AUTO|MANUAL|EMERGENCY Cambiar modo\n"
             + "  SET THRESHOLD <personas>   Umbral de ocupacion\n"
             + "  SET DISTANCE <cm>          Umbral del sensor\n"
             + "  STATUS                     Estado del sistema\n"
             + "  HELP                       Esta ayuda\n"
             + "  EXIT                       Salir\n";
    }
}
