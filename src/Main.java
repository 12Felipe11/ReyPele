import presentacion.ConsoleUI;
import aplicacion.StadiumController;
import aplicacion.PersistenceController;
import dominio.StadiumFacade;
import dominio.sensor.EntryCounterSensor;
import dominio.actuator.AlarmZoneActuator;
import dominio.actuator.LightingZoneActuator;
import dominio.factory.DeviceFactory;
import dominio.factory.SimulatedHardwareFactory;
import dominio.factory.RealHardwareFactory;
import dominio.strategy.AutoModeStrategy;
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

        // ── 2. Crear dispositivos ──────────────────────────────────────────
        EntryCounterSensor sensorEntrada = factory.createEntryCounterSensor(
                "ENTRY-01", "Puerta Norte", MAX_CAPACITY);
        AlarmZoneActuator  alarma        = factory.createAlarmZoneActuator(
                "ALARM-01", "Zona General");
        LightingZoneActuator luces       = factory.createLightingZoneActuator(
                "LIGHT-01", "Zona General");

        // ── 3. Configurar fachada ──────────────────────────────────────────
        StadiumFacade facade = new StadiumFacade();
        facade.addSensor(sensorEntrada);
        facade.addActuator(alarma);
        facade.addActuator(luces);

        // ── 4. Observadores ────────────────────────────────────────────────
        StadiumController    controller  = new StadiumController(facade);
        ConsoleUI            consola     = new ConsoleUI();
        PersistenceController persistencia = new PersistenceController();

        sensorEntrada.addObserver(controller);   // logica de negocio
        sensorEntrada.addObserver(consola);      // presentacion
        sensorEntrada.addObserver(persistencia); // persistencia en BD

        // ── 5. Modo inicial ────────────────────────────────────────────────
        System.out.println("Inicializando en modo AUTOMATICO...");
        facade.changeMode(new AutoModeStrategy());
        printEstado(facade, alarma, luces);

        // ── 6. Ejecucion segun modo ────────────────────────────────────────
        if (useRealHardware) {
            runRealHardwareLoop(sensorEntrada, facade, alarma, luces, persistencia);
        } else {
            runSimulation(sensorEntrada, facade, alarma, luces, persistencia);
        }

        // ── 7. Limpieza al cerrar ──────────────────────────────────────────
        infraestructura.db.DatabaseManager.getInstance().close();
    }

    // Modo REAL: lee el sensor del Arduino en loop hasta que el usuario detenga el programa
    private static void runRealHardwareLoop(EntryCounterSensor sensor,
                                             StadiumFacade facade,
                                             AlarmZoneActuator alarma,
                                             LightingZoneActuator luces,
                                             PersistenceController persistencia) {
        System.out.println("\nMonitoreo activo. Acerca una persona al sensor HC-SR04.");
        System.out.println("Presiona Ctrl+C para detener.\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n--- Estado final del sistema ---");
            printEstado(facade, alarma, luces);
            persistencia.printSummary();
            System.out.println("Sistema detenido.");
        }));

        while (true) {
            sensor.readValue(); // detecta nuevas entradas desde Arduino y notifica observers
            try {
                Thread.sleep(300); // consulta cada 300ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Modo SIMULADO: demo con entradas ficticias
    private static void runSimulation(EntryCounterSensor sensor,
                                       StadiumFacade facade,
                                       AlarmZoneActuator alarma,
                                       LightingZoneActuator luces,
                                       PersistenceController persistencia) {
        System.out.println("\n--- Fase 1: Entradas normales ---");
        for (int i = 1; i <= 3; i++) {
            sensor.registerEntry();
            System.out.printf("  Entrada registrada. Conteo: %d/%d%n",
                    sensor.getCurrentCount(), sensor.getMaxCapacity());
        }

        System.out.println("\n--- Fase 2: Se alcanza la capacidad maxima ---");
        for (int i = 4; i <= 6; i++) {
            sensor.registerEntry();
            System.out.printf("  Entrada registrada. Conteo: %d/%d%n",
                    sensor.getCurrentCount(), sensor.getMaxCapacity());
        }

        System.out.println("\n--- Estado final del sistema ---");
        printEstado(facade, alarma, luces);
        persistencia.printSummary();
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

    private static void printEstado(StadiumFacade facade,
                                     AlarmZoneActuator alarma,
                                     LightingZoneActuator luces) {
        System.out.println("  Modo actual:       " + facade.getCurrentMode().getModeName());
        System.out.println("  Alarma activa:     " + alarma.isActive());
        System.out.println("  Luces activas:     " + luces.isActive());
        System.out.println("  Intensidad luces:  " + luces.getIntensityPercent() + "%");
    }
}
