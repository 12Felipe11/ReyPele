package presentacion;

import dominio.Observer;

/**
 * Interfaz de consola (capa de presentación).
 * Observa eventos del sistema y los muestra al operador en formato legible.
 */
public class ConsoleUI implements Observer {

    @Override
    public void update(String eventType, Object data) {
        System.out.println("  [CONSOLA] >>> ALERTA: " + formatEvent(eventType, data));
    }

    private String formatEvent(String eventType, Object data) {
        switch (eventType) {
            case "CAPACITY_EXCEEDED":
                return "CAPACIDAD EXCEDIDA - Conteo actual: " + data;
            case "ENTRY_REGISTERED":
                Object[] payload = (Object[]) data;
                return "ENTRADA REGISTRADA - Sensor: " + payload[0]
                        + " | Ubicacion: " + payload[1]
                        + " | Total: " + payload[2];
            default:
                return eventType + " - " + data;
        }
    }
}
