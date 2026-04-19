import aplicacion.PersistenceController;
import aplicacion.StadiumController;
import dominio.StadiumFacade;
import dominio.actuator.AlarmZoneActuator;
import dominio.actuator.LightingZoneActuator;
import dominio.factory.DeviceFactory;
import dominio.factory.RealHardwareFactory;
import dominio.factory.SimulatedHardwareFactory;
import dominio.sensor.EntryCounterSensor;
import dominio.sensor.PresenceSensor;
import dominio.strategy.AutoModeStrategy;
import presentacion.CommandConsole;
import presentacion.ConsoleUI;

import java.util.Scanner;

public class Main {

    private static final int MAX_CAPACITY = 5;

    public static void main(String[] args) {
        System.out.println("=== SISTEMA DE CONTROL DE ESTADIO REYPELE ===\n");

        // ── 1. Seleccion de modo ───────────────────────────────────────────
        boolean useRealHardware = askHardwareMode();

        // ── 2. Fabrica de dispositivos ─────────────────────────────────────
        DeviceFactory factory = useRealHardware
                ? new RealHardwareFactory()
                : new SimulatedHardwareFactory();

        System.out.println("Modo hardware: " + (useRealHardware ? "REAL (Arduino)" : "SIMULADO"));

        // ── 3. Crear dispositivos ──────────────────────────────────────────
        EntryCounterSensor sensorEntrada = factory.createEntryCounterSensor(
                "ENTRY-01", "Puerta Norte", MAX_CAPACITY);
        PresenceSensor sensorPresencia   = factory.createPresenceSensor(
                "PRESENCE-01", "Puerta Norte");
        AlarmZoneActuator  alarma        = factory.createAlarmZoneActuator(
                "ALARM-01", "Zona General");
        LightingZoneActuator luces       = factory.createLightingZoneActuator(
                "LIGHT-01", "Zona General");

        // ── 4. Configurar fachada ──────────────────────────────────────────
        StadiumFacade facade = new StadiumFacade();
        facade.addSensor(sensorEntrada);
        facade.addSensor(sensorPresencia);
        facade.addActuator(alarma);
        facade.addActuator(luces);

        // ── 5. Observadores ────────────────────────────────────────────────
        StadiumController     controller  = new StadiumController(facade);
        ConsoleUI             consola     = new ConsoleUI();
        PersistenceController persistencia = new PersistenceController();

        sensorEntrada.addObserver(controller);
        sensorEntrada.addObserver(consola);
        sensorEntrada.addObserver(persistencia);

        // ── 6. Modo inicial ────────────────────────────────────────────────
        System.out.println("Inicializando en modo AUTOMATICO...\n");
        facade.changeMode(new AutoModeStrategy());

        // ── 7. Consola interactiva en hilo separado ────────────────────────
        CommandConsole commandConsole = new CommandConsole(facade);
        Thread consoleThread = new Thread(commandConsole, "command-console");
        consoleThread.setDaemon(true);
        consoleThread.start();

        // ── 8. Loop principal ──────────────────────────────────────────────
        if (useRealHardware) {
            runRealHardwareLoop(sensorEntrada, sensorPresencia);
        } else {
            runSimulation(sensorEntrada);
        }

        // ── 9. Limpieza al cerrar ──────────────────────────────────────────
        persistencia.printSummary();
        infraestructura.db.DatabaseManager.getInstance().close();
    }

    // Modo REAL: polling continuo al Arduino; la consola interactiva corre en paralelo
    private static void runRealHardwareLoop(EntryCounterSensor entrySensor,
                                             PresenceSensor presenceSensor) {
        System.out.println("Monitoreo activo. Escribe READ para ver el estado, EXIT para salir.\n");
        while (true) {
            entrySensor.readValue();
            presenceSensor.readValue();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Modo SIMULADO: registra entradas ficticias para demostrar el flujo
    private static void runSimulation(EntryCounterSensor sensor) {
        System.out.println("Simulacion en curso. Tambien puedes escribir comandos (READ, MODE, etc.)\n");

        pause(1000);
        System.out.println("--- Simulando entradas ---");
        for (int i = 1; i <= 6; i++) {
            pause(600);
            sensor.registerEntry();
            System.out.printf("  Entrada simulada %d/%d%n",
                    sensor.getCurrentCount(), sensor.getMaxCapacity());
        }

        System.out.println("\nSimulacion terminada. Escribe READ para ver el estado final o EXIT para salir.");
        // El hilo de consola sigue activo esperando comandos
        try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
    }

    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static boolean askHardwareMode() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Selecciona el modo de operacion:");
        System.out.println("  1 - Simulado (sin Arduino)");
        System.out.println("  2 - Real (Arduino conectado)");
        System.out.print("Opcion: ");

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equals("1")) return false;
            if (input.equals("2")) return true;
            System.out.print("Opcion invalida. Ingresa 1 o 2: ");
        }
    }
}
