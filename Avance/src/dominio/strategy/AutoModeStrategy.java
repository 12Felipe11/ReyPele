package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

public class AutoModeStrategy implements IStadiumModeStrategy {

    @Override public String getModeName() { return "AUTO"; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();

        // Detección de presencia
        boolean presence = f.getDistanceSensor().isPresenceDetected();
        if (presence) {
            actions.add("Presencia detectada");
        } else {
            actions.add("Sin presencia");
        }

        // HU-07: alarma por sobreocupacion
        int count = f.getEntrySensor().getCount();
        int threshold = f.getConfig().getOccupancyThreshold();
        if (count > threshold) {
            if (!f.getAlarmActuator().isActive()) {
                f.setAlarm(true);
                actions.add("Entradas (" + count + ") > umbral (" + threshold + ") -> ALARMA");
            }
        } else {
            if (f.getAlarmActuator().isActive()) {
                f.setAlarm(false);
                actions.add("Ocupacion normalizada -> Alarma desactivada");
            }
        }

        return actions;
    }

    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return false; }
}
