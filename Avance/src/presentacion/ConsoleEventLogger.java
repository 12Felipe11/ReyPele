package presentacion;

import dominio.event.EventType;
import dominio.event.IStadiumObserver;
import dominio.event.StadiumEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Observador que imprime en consola los eventos relevantes de la fachada.
 * Filtra SENSOR_READ para no saturar la consola con cada lectura.
 */
public class ConsoleEventLogger implements IStadiumObserver {

    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

    @Override
    public void onEvent(StadiumEvent event) {
        if (event.getType() == EventType.SENSOR_READ) return;
        System.out.printf("  [EVENTO %s %s] %s%n",
                fmt.format(new Date(event.getTimestamp())),
                event.getType(),
                event.getDescription());
    }
}
