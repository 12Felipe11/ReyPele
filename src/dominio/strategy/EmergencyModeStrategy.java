package dominio.strategy;

import dominio.StadiumFacade;
import dominio.actuator.Actuator;

/**
 * Activa todos los actuadores: alarmas sonoras y luces al 100%
 * para facilitar la evacuación segura del estadio.
 */
public class EmergencyModeStrategy implements IStadiumModeStrategy {

    @Override
    public void execute(StadiumFacade facade) {
        System.out.println("  [EMERGENCIA] Activando protocolo de emergencia...");
        for (Actuator actuator : facade.getActuators()) {
            actuator.activate();
        }
    }

    @Override
    public String getModeName() {
        return "EMERGENCIA";
    }
}
