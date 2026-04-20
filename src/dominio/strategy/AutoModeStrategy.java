package dominio.strategy;

import dominio.StadiumFacade;
import dominio.actuator.Actuator;

/**
 * Modo normal de operación: cada actuador decide su comportamiento
 * via applyAutoMode(), sin que la estrategia conozca los tipos concretos.
 */
public class AutoModeStrategy implements IStadiumModeStrategy {

    @Override
    public void execute(StadiumFacade facade) {
        System.out.println("  [AUTO] Configurando modo automatico...");
        for (Actuator actuator : facade.getActuators()) {
            actuator.applyAutoMode();
        }
    }

    @Override
    public String getModeName() {
        return "AUTOMATICO";
    }
}
