package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

/**
 * Modo EMERGENCY (HU-11): alarma + luces siempre al maximo.
 */
public class EmergencyModeStrategy implements IStadiumModeStrategy {

    @Override public String getModeName() { return "EMERGENCY"; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();
        if (!f.getAlarmActuator().isActive()) {
            f.setAlarm(true);
            actions.add("[EMERGENCIA] Alarma ACTIVADA");
        }
        if (f.getLightingActuator().getIntensity() < 100) {
            f.setLight(100);
            actions.add("[EMERGENCIA] Luces al 100%");
        }
        return actions;
    }

    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return false; }
}
