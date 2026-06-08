package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

/**
 * Modo EMERGENCY (HU-11): alarma + LEDs ROJOS con parpadeo.
 * Simula rutas de evacuacion con intensidad alternante.
 */
public class EmergencyModeStrategy implements IStadiumModeStrategy {

    private static final String[] ZONES = {"NORTE", "SUR", "ORIENTAL", "OCCIDENTAL", "CANCHA"};
    private int blinkTick = 0;

    @Override public String getModeName() { return "EMERGENCY"; }
    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return false; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();

        if (!f.getAlarmActuator().isActive()) {
            f.setAlarm(true);
            actions.add("[EMERGENCIA] Alarma ACTIVADA");
        }

        // Parpadeo: tick par = encendido, tick impar = atenuado
        boolean encendido = (blinkTick % 2 == 0);
        int intensidad = encendido ? 100 : 15;

        for (String z : ZONES) {
            f.setZoneColor(z, 255, 0, 0);       // ROJO
            f.setZoneLight(z, intensidad);
        }

        actions.add("[EMERGENCIA] LEDs ROJOS — " + (encendido ? "ON" : "dim") + " — EVACUESE");
        blinkTick++;
        return actions;
    }
}
