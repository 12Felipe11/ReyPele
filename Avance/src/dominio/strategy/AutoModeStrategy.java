package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

public class AutoModeStrategy implements IStadiumModeStrategy {

    @Override public String getModeName() { return "AUTO"; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();

        // Accion automatica: baja iluminacion -> encender luces
        int light = f.getLightingActuator().getIntensity();
        if (light < 30 && !f.getLightingActuator().isActive()) {
            f.setLight(100);
            actions.add("Baja iluminacion detectada -> LUCES ON");
        }

        // Accion automatica: capacidad del estadio completa -> activar alarma
        int count = f.getEntrySensor().getCount();
        int threshold = f.getConfig().getOccupancyThreshold();
        if (count >= threshold) {
            if (!f.getAlarmActuator().isActive()) {
                f.setAlarm(true);
                actions.add("umbral alcanzado, ALARM ON");
            }
        } else {
            if (f.getAlarmActuator().isActive()) {
                f.setAlarm(false);
                actions.add("Ocupacion normalizada -> ALARM OFF");
            }
        }

        return actions;
    }

    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return false; }
}
