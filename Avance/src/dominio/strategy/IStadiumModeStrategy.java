package dominio.strategy;

import dominio.StadiumFacade;
import java.util.List;

/**
 * Interfaz Strategy para modos de operacion (HU-08).
 */
public interface IStadiumModeStrategy {
    String getModeName();
    List<String> evaluate(StadiumFacade facade);
    boolean canControlLight();
    boolean canControlAlarm();
}
