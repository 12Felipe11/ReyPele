package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

/** Modo PARTIDO: todas las zonas al 100% con luz blanca. */
public class MatchModeStrategy implements IStadiumModeStrategy {

    private static final String[] ZONES = {"NORTE", "SUR", "ORIENTAL", "OCCIDENTAL", "CANCHA"};
    private boolean initialized = false;

    @Override public String getModeName() { return "PARTIDO"; }
    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return true; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();
        if (!initialized) {
            for (String z : ZONES) {
                f.setZoneColor(z, 255, 255, 255);
                f.setZoneLight(z, 100);
            }
            initialized = true;
            actions.add("[PARTIDO] Todas las zonas al 100% - Luz blanca");
        }
        // Mantener alarma si hay sobreocupacion
        int count = f.getEntrySensor().getCount();
        int threshold = f.getConfig().getOccupancyThreshold();
        if (count > threshold && !f.getAlarmActuator().isActive()) {
            f.setAlarm(true);
            actions.add("[PARTIDO] Sobreocupacion detectada -> ALARMA");
        }
        return actions;
    }
}
