package presentacion;

import aplicacion.StadiumController;
import java.util.Scanner;

/**
 * Consola interactiva (HU-02).
 * Muestra datos formateados y legibles al operador.
 */
public class ConsoleUI {

    private final StadiumController controller;
    private final Scanner scanner;

    public ConsoleUI(StadiumController controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("=========================================================");
        System.out.println("  SISTEMA DE CONTROL DE ESTADIO INTELIGENTE");
        System.out.println("  Escriba READ para leer sensores | HELP para ayuda");
        System.out.println("=========================================================");

        while (true) {
            System.out.print("\n> ");
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String result = controller.processCommand(input);
            if (result == null) {
                System.out.println("  Cerrando sistema...");
                break;
            }
            System.out.print(result);
        }
    }
}
