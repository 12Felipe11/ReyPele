package aplicacion;

import dominio.Observer;
import dominio.StadiumFacade;
import dominio.strategy.EmergencyModeStrategy;

public class StadiumController implements Observer {

    private final StadiumFacade facade;

    public StadiumController(StadiumFacade facade) {
        this.facade = facade;
    }

    @Override
    public void update(String eventType, Object data) {
        switch (eventType) {
            case "CAPACITY_EXCEEDED":
                handleCapacityExceeded((int) data);
                break;
            case "ENTRY_REGISTERED":
                // Manejado por PersistenceController; el controlador no necesita actuar
                break;
            default:
                System.out.println("  [CONTROLLER] Evento no manejado: " + eventType);
                break;
        }
    }

    private void handleCapacityExceeded(int count) {
        System.out.println("  [CONTROLLER] Capacidad excedida (" + count
                + "). Activando modo EMERGENCIA.");
        facade.changeMode(new EmergencyModeStrategy());
    }
}
