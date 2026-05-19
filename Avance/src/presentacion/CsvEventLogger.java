package presentacion;

import dominio.event.IStadiumObserver;
import dominio.event.StadiumEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Observador que registra cada evento en un archivo CSV (modo append).
 * Demuestra que el patron Observer permite anadir nuevos suscriptores sin
 * tocar la fachada ni el resto del sistema.
 */
public class CsvEventLogger implements IStadiumObserver {

    private final PrintWriter writer;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public CsvEventLogger(String filename) throws IOException {
        File file = new File(filename);
        boolean isNew = !file.exists() || file.length() == 0;
        this.writer = new PrintWriter(new FileWriter(file, true), true);
        if (isNew) {
            writer.println("timestamp,type,description");
        }
    }

    @Override
    public void onEvent(StadiumEvent event) {
        String ts = fmt.format(new Date(event.getTimestamp()));
        String desc = event.getDescription().replace(",", ";");
        writer.printf("%s,%s,%s%n", ts, event.getType(), desc);
    }

    public void close() {
        writer.close();
    }
}
