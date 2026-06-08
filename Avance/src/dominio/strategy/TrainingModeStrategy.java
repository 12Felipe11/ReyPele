package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

/** Modo ENTRENAMIENTO: solo cancha al 80%, tribunas al minimo. */
public class TrainingModeStrategy implements IStadiumModeStrategy {

    private static final String[] TRIBUNAS = {"NORTE", "SUR", "ORIENTAL", "OCCIDENTAL"};
    private boolean initialized = false;

    @Override public String getModeName() { return "ENTRENAMIENTO"; }
    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return true; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();
        if (!initialized) {
            f.setZoneColor("CANCHA", 255, 255, 240);
            f.setZoneLight("CANCHA", 80);
            for (String z : TRIBUNAS) {
                f.setZoneColor(z, 80, 100, 200);
                f.setZoneLight(z, 10);
            }
            initialized = true;
            actions.add("[ENTRENAMIENTO] Cancha activa al 80%");
            actions.add("[ENTRENAMIENTO] Tribunas atenuadas al 10%");
        }
        return actions;
    }
}
