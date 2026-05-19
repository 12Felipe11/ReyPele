package dominio.event;

/**
 * Tipos de eventos que la fachada puede emitir a sus observadores.
 */
public enum EventType {
    SENSOR_READ,        // se completo una lectura de sensores
    ENTRY_DETECTED,     // nueva persona entro al estadio
    ALARM_CHANGED,      // alarma encendida o apagada
    LIGHT_CHANGED,      // intensidad de luz modificada
    MODE_CHANGED,       // cambio de modo de operacion
    THRESHOLD_CHANGED   // cambio de umbral (ocupacion o distancia)
}
