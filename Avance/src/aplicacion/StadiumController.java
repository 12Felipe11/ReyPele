package aplicacion;

import dominio.SensorData;
import dominio.StadiumFacade;
import dominio.strategy.*;

import java.util.List;

/**
 * Controlador de aplicacion.
 * Procesa todos los comandos del operador (HU-01 a HU-14 + zonas + modos inteligentes).
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
            case "ZONE":   return handleZone(parts);
            case "COLOR":  return handleColor(parts);
            case "SHOW":   return handleShow();
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
        sb.append(String.format("  Entradas:      %d / %d (umbral)%n",
                data.getEntryCount(), facade.getConfig().getOccupancyThreshold()));
        sb.append(String.format("  Luces:         %d%%%n", data.getLightIntensity()));

        List<String> actions = facade.evaluateRules();
        if (!actions.isEmpty()) {
            sb.append("  --- ACCIONES [")
              .append(facade.getCurrentMode().getModeName()).append("] ---\n");
            for (String a : actions) sb.append("  > ").append(a).append("\n");
        }
        return sb.toString();
    }

    /** HU-04: LIGHT <0-100> o LIGHT ON/OFF */
    private String handleLight(String[] parts) {
        if (parts.length < 2)
            return "  Uso: LIGHT <0-100> | LIGHT ON | LIGHT OFF";
        String arg = parts[1].toUpperCase();
        int intensity;
        if ("ON".equals(arg))       intensity = 100;
        else if ("OFF".equals(arg)) intensity = 0;
        else {
            try { intensity = Integer.parseInt(arg); }
            catch (NumberFormatException e) { return "  Valor invalido."; }
        }
        // intensity == 0 (apagar todo) siempre se permite, cualquier modo
        if (intensity > 0 && !facade.getCurrentMode().canControlLight())
            return "  No permitido en modo " + facade.getCurrentMode().getModeName();
        facade.setLight(intensity);
        return "  Luces (todas las zonas): " + intensity + "%";
    }

    /** ZONE <nombre|ALL> <0-100> */
    private String handleZone(String[] parts) {
        if (parts.length < 3)
            return "  Uso: ZONE <NORTE|SUR|ORIENTAL|OCCIDENTAL|CANCHA|ALL> <0-100>";
        if (!facade.getCurrentMode().canControlLight())
            return "  No permitido en modo " + facade.getCurrentMode().getModeName();

        String zoneName = parts[1].toUpperCase();
        int intensity;
        try { intensity = Integer.parseInt(parts[2]); }
        catch (NumberFormatException e) { return "  Intensidad invalida (0-100)."; }

        if ("ALL".equals(zoneName)) {
            facade.setAllZones(intensity);
            return "  Todas las zonas: " + intensity + "%";
        }
        boolean ok = facade.setZoneLight(zoneName, intensity);
        return ok ? "  Zona " + zoneName + ": " + intensity + "%" : "  Zona no reconocida: " + zoneName;
    }

    /** COLOR <zona|ALL> <r> <g> <b>  — valores 0-255 */
    private String handleColor(String[] parts) {
        if (parts.length < 5)
            return "  Uso: COLOR <NORTE|SUR|ORIENTAL|OCCIDENTAL|CANCHA|ALL> <r> <g> <b>";
        if (!facade.getCurrentMode().canControlLight())
            return "  No permitido en modo " + facade.getCurrentMode().getModeName();

        String zoneName = parts[1].toUpperCase();
        int r, g, b;
        try {
            r = Integer.parseInt(parts[2]);
            g = Integer.parseInt(parts[3]);
            b = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) { return "  Valores RGB invalidos (0-255)."; }

        if ("ALL".equals(zoneName)) {
            facade.setAllZonesColor(r, g, b);
            return String.format("  Todas las zonas: rgb(%d,%d,%d)", r, g, b);
        }
        boolean ok = facade.setZoneColor(zoneName, r, g, b);
        return ok ? String.format("  Zona %s: rgb(%d,%d,%d)", zoneName, r, g, b)
                  : "  Zona no reconocida: " + zoneName;
    }

    /** SHOW — activa el espectaculo previo al partido */
    private String handleShow() {
        facade.changeMode(new PreMatchShowStrategy());
        return "  Show previo al partido iniciado. Use MODE MANUAL para detener.";
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

    /** HU-08 — ahora incluye PARTIDO, ENTRENAMIENTO, EVENTO, SHOW */
    private String handleMode(String[] parts) {
        if (parts.length < 2)
            return "  Uso: MODE AUTO|MANUAL|EMERGENCY|PARTIDO|ENTRENAMIENTO|EVENTO|SHOW";
        String name = parts[1].toUpperCase();
        switch (name) {
            case "AUTO":          facade.changeMode(new AutoModeStrategy());        break;
            case "MANUAL":        facade.changeMode(new ManualModeStrategy());      break;
            case "EMERGENCY":     facade.changeMode(new EmergencyModeStrategy());   break;
            case "PARTIDO":       facade.changeMode(new MatchModeStrategy());       break;
            case "ENTRENAMIENTO": facade.changeMode(new TrainingModeStrategy());    break;
            case "EVENTO":        facade.changeMode(new EventModeStrategy());       break;
            case "SHOW":          facade.changeMode(new PreMatchShowStrategy());    break;
            default: return "  Modo invalido. Opciones: AUTO, MANUAL, EMERGENCY, PARTIDO, ENTRENAMIENTO, EVENTO, SHOW";
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
        sb.append(String.format("  Luces global:    %d%%%n", facade.getLightingActuator().getIntensity()));
        sb.append("  --- ZONAS ---\n");
        facade.getZones().forEach((name, zone) ->
            sb.append(String.format("  %-12s %3d%%  %s%n", name, zone.getIntensity(), zone.getColorHex()))
        );
        sb.append(String.format("  Distancia:       %.1f cm%n", facade.getDistanceSensor().getDistanceCm()));
        sb.append(String.format("  Presencia:       %s%n", facade.getDistanceSensor().isPresenceDetected() ? "SI" : "NO"));
        sb.append(String.format("  Entradas:        %d%n", facade.getEntrySensor().getCount()));
        sb.append(String.format("  Hardware:        %s%n", facade.getHardware().isConnected() ? "CONECTADO" : "DESCONECTADO"));
        return sb.toString();
    }

    private String getHelp() {
        return "  --- COMANDOS (ReyPele) ---\n"
             + "  READ                                     Leer sensores\n"
             + "  LIGHT <0-100|ON|OFF>                     Luces global\n"
             + "  ZONE <NORTE|SUR|ORIENTAL|OCCIDENTAL|CANCHA|ALL> <0-100>\n"
             + "                                           Control por zona\n"
             + "  COLOR <zona|ALL> <r> <g> <b>             Color de zona (0-255)\n"
             + "  SHOW                                     Show previo al partido\n"
             + "  ALARM ON|OFF                             Alarma\n"
             + "  MODE AUTO|MANUAL|EMERGENCY|PARTIDO|ENTRENAMIENTO|EVENTO|SHOW\n"
             + "                                           Cambiar modo\n"
             + "  SET THRESHOLD <personas>                 Umbral de ocupacion\n"
             + "  SET DISTANCE <cm>                        Umbral del sensor\n"
             + "  STATUS                                   Estado del sistema\n"
             + "  HELP                                     Esta ayuda\n"
             + "  EXIT                                     Salir\n";
    }
}
