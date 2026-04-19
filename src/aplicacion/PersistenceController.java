package aplicacion;

import dominio.Observer;
import infraestructura.db.EntryRepository;

/**
 * Observador que persiste eventos del estadio en la base de datos SQLite.
 * Se registra en los mismos sensores que StadiumController y ConsoleUI.
 *
 * Eventos manejados:
 *   CAPACITY_EXCEEDED  → guarda evento de sistema con el conteo final
 *   ENTRY_REGISTERED   → guarda cada entrada individual (si el sensor lo emite)
 */
public class PersistenceController implements Observer {

    private final EntryRepository entryRepository;

    public PersistenceController() {
        this.entryRepository = new EntryRepository();
    }

    public PersistenceController(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Override
    public void update(String eventType, Object data) {
        switch (eventType) {
            case "CAPACITY_EXCEEDED":
                String countData = "count=" + data;
                entryRepository.saveSystemEvent("CAPACITY_EXCEEDED", countData);
                System.out.println("[DB] Evento persistido: CAPACITY_EXCEEDED con " + countData);
                break;

            case "ENTRY_REGISTERED":
                // Emitido por EntryCounterSensor al registrar una entrada
                // data es un Object[] { sensorId, location, totalCount }
                if (data instanceof Object[]) {
                    Object[] payload = (Object[]) data;
                    String sensorId  = (String)  payload[0];
                    String location  = (String)  payload[1];
                    int    count     = (Integer) payload[2];
                    entryRepository.saveEntry(sensorId, location, count);
                }
                break;

            case "MODE_CHANGED":
                if (data != null) {
                    entryRepository.saveModeChange(data.toString());
                }
                break;

            default:
                // Otros eventos se guardan como eventos de sistema genericos
                entryRepository.saveSystemEvent(eventType, data != null ? data.toString() : "");
                break;
        }
    }

    /** Imprime un resumen de la sesion leyendo directamente de la BD. */
    public void printSummary() {
        entryRepository.printSessionSummary();
    }

    public EntryRepository getEntryRepository() {
        return entryRepository;
    }
}
