package dominio.event;

/**
 * Contrato del patron Observer.
 * Cualquier componente interesado en cambios del estadio implementa esta
 * interfaz y se suscribe via StadiumFacade.addObserver(...).
 */
public interface IStadiumObserver {
    void onEvent(StadiumEvent event);
}
