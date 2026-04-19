package dominio.strategy;

import dominio.StadiumFacade;

/**
 * Estrategia de modo MANUAL.
 * No ejecuta acciones automáticas; el operador controla todo.
 */
public class ManualModeStrategy implements IStadiumModeStrategy {

    @Override
    public void execute(StadiumFacade facade) {
        System.out.println("  [MANUAL] Modo manual activo. Sin acciones automaticas.");
    }

    @Override
    public String getModeName() {
        return "MANUAL";
    }
}
