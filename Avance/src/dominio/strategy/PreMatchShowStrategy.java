package dominio.strategy;

import dominio.StadiumFacade;
import java.util.ArrayList;
import java.util.List;

/**
 * Modo SHOW: espectaculo previo al partido.
 * Secuencia: ola verde → amarillo global → rojo global → flash blanco
 *            → ola luminosa → apagado → encendido progresivo → repite.
 */
public class PreMatchShowStrategy implements IStadiumModeStrategy {

    private static final String[] ZONES = {"NORTE", "SUR", "ORIENTAL", "OCCIDENTAL", "CANCHA"};
    private int tick = 0;

    @Override public String getModeName() { return "SHOW"; }
    @Override public boolean canControlLight() { return false; }
    @Override public boolean canControlAlarm() { return true; }

    @Override
    public List<String> evaluate(StadiumFacade f) {
        List<String> actions = new ArrayList<>();
        int phase = tick % 20;

        if (phase < 5) {
            // Ola verde secuencial
            String zona = ZONES[phase];
            f.setZoneColor(zona, 30, 220, 80);
            f.setZoneLight(zona, 100);
            if (phase == 0) f.setZoneLight("CANCHA", 0);
            actions.add("[SHOW] Ola verde → " + zona);

        } else if (phase == 5) {
            // Todas amarillo
            for (String z : ZONES) { f.setZoneColor(z, 255, 200, 0); f.setZoneLight(z, 100); }
            actions.add("[SHOW] Estadio → AMARILLO");

        } else if (phase == 6) {
            // Todas rojo
            for (String z : ZONES) { f.setZoneColor(z, 255, 30, 30); f.setZoneLight(z, 100); }
            actions.add("[SHOW] Estadio → ROJO");

        } else if (phase == 7) {
            // Flash blanco maximo
            for (String z : ZONES) { f.setZoneColor(z, 255, 255, 255); f.setZoneLight(z, 100); }
            actions.add("[SHOW] FLASH BLANCO — Estadio al maximo");

        } else if (phase >= 8 && phase < 13) {
            // Ola luminosa: una zona llena, el resto tenue
            int waveIdx = phase - 8;
            for (int i = 0; i < ZONES.length; i++) {
                f.setZoneLight(ZONES[i], i == waveIdx ? 100 : 15);
            }
            actions.add("[SHOW] Ola luminosa → " + ZONES[waveIdx]);

        } else if (phase == 13) {
            // Apagado
            for (String z : ZONES) f.setZoneLight(z, 0);
            actions.add("[SHOW] Pausa...");

        } else {
            // Encendido progresivo (fases 14-19 → 25% → 50% → 75% → 100% → espera)
            int step = phase - 14;
            if (step < 4) {
                int pct = (step + 1) * 25;
                for (String z : ZONES) { f.setZoneColor(z, 255, 255, 255); f.setZoneLight(z, pct); }
                actions.add("[SHOW] Encendido progresivo " + pct + "%");
            }
        }

        tick++;
        return actions;
    }
}
