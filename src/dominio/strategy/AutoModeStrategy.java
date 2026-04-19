package dominio.strategy;

import dominio.StadiumFacade;
import dominio.actuator.Actuator;
import dominio.actuator.AlarmZoneActuator;

/**
 * Modo normal de operación: las alarmas se desactivan,
 * los demás actuadores mantienen su estado actual.
 */
public class AutoModeStrategy implements IStadiumModeStrategy {

    @Override
    public void execute(StadiumFacade facade) {
        System.out.println("  [AUTO] Configurando modo automatico...");
        for (Actuator actuator : facade.getActuators()) {
            if (actuator instanceof AlarmZoneActuator) {
                actuator.deactivate();
            }
        }
    }

    @Override
    public String getModeName() {
        return "AUTOMATICO";
    }
}
