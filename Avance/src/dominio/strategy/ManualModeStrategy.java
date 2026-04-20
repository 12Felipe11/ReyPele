package dominio.strategy;

import dominio.StadiumFacade;
import java.util.Collections;
import java.util.List;

/**
 * Modo MANUAL (HU-09): sin automatizacion, solo comandos directos.
 */
public class ManualModeStrategy implements IStadiumModeStrategy {
    @Override public String getModeName() { return "MANUAL"; }
    @Override public List<String> evaluate(StadiumFacade f) { return Collections.emptyList(); }
    @Override public boolean canControlLight() { return true; }
    @Override public boolean canControlAlarm() { return true; }
}
