package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

/** Modo EVENTO: efecto cromatico rotando entre zonas. */
public class EventModeStrategy implements IStadiumModeStrategy {

    private static final String[] ZONES = {"NORTE", "SUR", "ORIENTAL", "OCCIDENTAL", "CANCHA"};
    private static final int[][] COLORS = {
        {124,  58, 237},  // violeta
        {  6, 182, 212},  // cyan
        { 16, 185, 129},  // verde esmeralda
        {245, 158,  11},  // ambar
        {239,  68,  68},  // rojo
    };
    private int tick = 0;

    @Override public String getModeName() { return "EVENTO"; }
    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return true; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();
        int offset = tick % ZONES.length;
        for (int i = 0; i < ZONES.length; i++) {
            int[] c = COLORS[(i + offset) % COLORS.length];
            f.setZoneColor(ZONES[i], c[0], c[1], c[2]);
            f.setZoneLight(ZONES[i], 85);
        }
        actions.add("[EVENTO] Efecto cromatico - paso " + (tick + 1));
        tick++;
        return actions;
    }
}
