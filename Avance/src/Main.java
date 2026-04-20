import aplicacion.StadiumController;
import dominio.StadiumFacade;
import dominio.factory.DeviceFactory;
import dominio.factory.RealHardwareFactory;
import dominio.factory.SimulatedHardwareFactory;
import dominio.persistence.ISensorRepository;
import infraestructura.ArduinoPortScanner;
import infraestructura.IHardwareComm;
import infraestructura.NoOpSensorRepository;
import infraestructura.SqliteSensorRepository;
import presentacion.ConsoleUI;
import presentacion.WebDashboardServer;

import java.util.Scanner;

/**
 * Punto de entrada.
 * Arranca:
 *  - Dashboard web (HTML) en http://localhost:8080 (se abre solo).
 *  - Consola interactiva (HU-01..HU-14).
 * En modo Arduino, detecta el puerto automaticamente.
 */
public class Main {

    private static final int DASHBOARD_PORT = 8080;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== ESTADIO INTELIGENTE ===");
        System.out.println("  1. Simulador");
        System.out.println("  2. Arduino (serial)");
        System.out.print("Opcion [1]: ");

        String option = scanner.nextLine().trim();

        DeviceFactory factory;
        if ("2".equals(option)) {
            String port = ArduinoPortScanner.detect();
            if (port != null) {
                System.out.print("Arduino detectado en " + port + ". Usar ese puerto? [S/n]: ");
                String ans = scanner.nextLine().trim().toLowerCase();
                if (ans.startsWith("n")) {
                    System.out.print("Puerto manual (ej: COM3 / /dev/ttyACM0): ");
                    port = scanner.nextLine().trim();
                }
            } else {
                System.out.print("Puerto manual (ej: COM3 / /dev/ttyACM0): ");
                port = scanner.nextLine().trim();
            }
            if (port.isEmpty()) {
                System.err.println("No se especifico puerto. Abortando.");
                return;
            }
            factory = new RealHardwareFactory(port);
        } else {
            factory = new SimulatedHardwareFactory();
        }

        IHardwareComm hardware = factory.createHardwareComm();
        hardware.connect();

        if (!hardware.isConnected()) {
            System.err.println("No se pudo conectar. Abortando.");
            return;
        }

        ISensorRepository repository = buildRepository();
        StadiumFacade facade = new StadiumFacade(hardware, repository);
        StadiumController controller = new StadiumController(facade);

        WebDashboardServer web = new WebDashboardServer(facade, controller, DASHBOARD_PORT);
        try {
            web.start();
        } catch (Exception e) {
            System.err.println("  [WEB] No se pudo iniciar el dashboard: " + e.getMessage());
        }

        ConsoleUI ui = new ConsoleUI(controller, scanner);
        ui.run();

        web.stop();
        hardware.disconnect();
        scanner.close();
    }

    /** Persistencia local en SQLite (archivo stadium.db). */
    private static ISensorRepository buildRepository() {
        String dbPath = System.getProperty("stadium.db", "stadium.db");
        try {
            System.out.println("  [SQLITE] Abriendo base de datos en " + dbPath);
            return new SqliteSensorRepository(dbPath);
        } catch (Exception e) {
            System.err.println("  [SQLITE] Error: " + e.getMessage()
                    + ". Persistencia deshabilitada.");
            return new NoOpSensorRepository();
        }
    }
}
