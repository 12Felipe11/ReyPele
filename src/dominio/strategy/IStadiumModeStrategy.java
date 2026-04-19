package dominio.strategy;

import dominio.StadiumFacade;

/**
 * Cada modo define un comportamiento distinto al ejecutarse sobre la fachada.
 */
public interface IStadiumModeStrategy {

    void execute(StadiumFacade facade);

    String getModeName();
}
