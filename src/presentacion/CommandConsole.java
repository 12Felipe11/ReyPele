package presentacion;

import dominio.StadiumFacade;
import dominio.actuator.Actuator;
import dominio.sensor.Sensor;
import dominio.strategy.AutoModeStrategy;
import dominio.strategy.EmergencyModeStrategy;
import dominio.strategy.ManualModeStrategy;

import java.util.Scanner;

/**
 * Consola interactiva para el operador. Depende solo de StadiumFacade
 * y las abstracciones Sensor/Actuator, sin conocer clases concretas.
 *
 * Comandos:
 *   READ                        → estado actual de sensores y actuadores
 *   MODE AUTO|MANUAL|EMERGENCY  → cambia el modo de operacion
 *   ALARM ON|OFF                → controla la alarma por nombre de actuador
 *   EXIT                        → detiene el sistema
 */
public class CommandConsole implements Runnable {

    private final StadiumFacade facade;
    private volatile boolean running = true;

    public CommandConsole(StadiumFacade facade) {
        this.facade = facade;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (running) {
            System.out.print("\nCOMANDO> ");
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim().toUpperCase();
            if (input.isEmpty()) continue;

            processCommand(input);
        }
    }

    private void processCommand(String input) {
        if (input.equals("READ")) {
            executeRead();

        } else if (input.startsWith("MODE ")) {
            executeMode(input.substring(5).trim());

        } else if (input.equals("ALARM ON")) {
            facade.getActuators().forEach(a -> {
                if (a.getId().startsWith("ALARM")) a.activate();
            });
            System.out.println("[ALARM] Alarma activada.");

        } else if (input.equals("ALARM OFF")) {
            facade.getActuators().forEach(a -> {
                if (a.getId().startsWith("ALARM")) a.deactivate();
            });
            System.out.println("[ALARM] Alarma desactivada.");

        } else if (input.equals("EXIT")) {
            running = false;
            System.out.println("Cerrando sistema...");
            System.exit(0);

        } else if (input.equals("HELP")) {
            printHelp();

        } else {
            System.out.println("Comando no reconocido. Escribe HELP para ver los comandos.");
        }
    }

    // HU-01 y HU-02: cada sensor/actuador sabe describirse a si mismo
    private void executeRead() {
        facade.getSensors().forEach(Sensor::readValue);

        System.out.println();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      READ - ESTADO DEL SISTEMA       ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.printf( "║  Modo:        %-23s║%n", facade.getCurrentMode().getModeName());

        for (Sensor s : facade.getSensors())   System.out.printf("║%-38s║%n", s.getStatusLine());
        for (Actuator a : facade.getActuators()) System.out.printf("║%-38s║%n", a.getStatusLine());

        System.out.println("╚══════════════════════════════════════╝");
    }

    // HU-08: cambiar modo de operacion
    private void executeMode(String mode) {
        switch (mode) {
            case "AUTO":
                facade.changeMode(new AutoModeStrategy());
                System.out.println("[MODE] Modo cambiado a AUTOMATICO.");
                break;
            case "MANUAL":
                facade.changeMode(new ManualModeStrategy());
                System.out.println("[MODE] Modo cambiado a MANUAL.");
                break;
            case "EMERGENCY":
                facade.changeMode(new EmergencyModeStrategy());
                System.out.println("[MODE] Modo cambiado a EMERGENCIA.");
                break;
            default:
                System.out.println("Modo invalido. Usa: MODE AUTO | MODE MANUAL | MODE EMERGENCY");
        }
    }

    private void printHelp() {
        System.out.println();
        System.out.println("=== COMANDOS DISPONIBLES ===");
        System.out.println("  READ                  → ver estado actual de sensores");
        System.out.println("  MODE AUTO             → activar modo automatico");
        System.out.println("  MODE MANUAL           → activar modo manual");
        System.out.println("  MODE EMERGENCY        → activar modo emergencia");
        System.out.println("  ALARM ON / ALARM OFF  → controlar alarma manualmente");
        System.out.println("  EXIT                  → detener el sistema");
        System.out.println("============================");
    }

    public void stop() {
        running = false;
    }
}
