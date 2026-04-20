package dominio;

/**
 * Permite que componentes reaccionen a eventos del sistema sin acoplarse
 * directamente a la fuente del evento.
 */
public interface Observer {

    void update(String eventType, Object data);
}
