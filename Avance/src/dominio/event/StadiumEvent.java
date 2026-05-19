package dominio.event;

/**
 * Evento inmutable emitido por StadiumFacade hacia sus observadores.
 */
public class StadiumEvent {

    private final EventType type;
    private final String description;
    private final long timestamp;

    public StadiumEvent(EventType type, String description) {
        this.type = type;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
    }

    public EventType getType()     { return type; }
    public String getDescription() { return description; }
    public long getTimestamp()     { return timestamp; }
}
