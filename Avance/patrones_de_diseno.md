# Patrones de Diseno - Sistema de Control de Estadio Inteligente

Este documento identifica los patrones de diseno de software presentes en el
proyecto **Avance** (Sistema ReyPele de control de estadio).  Cada patron se
vincula a archivos y clases reales del codigo fuente, no a ejemplos aislados.

---

## Resumen

| # | Patron | Tipo | Clase principal | Ruta |
|---|--------|------|-----------------|------|
| 1 | **Facade** | Estructural | `StadiumFacade` | `src/dominio/StadiumFacade.java` |
| 2 | **Strategy** | Comportamiento | `IStadiumModeStrategy` | `src/dominio/strategy/` |
| 3 | **Abstract Factory** | Creacional | `DeviceFactory` | `src/dominio/factory/` |
| 4 | **Adapter** | Estructural | `ArduinoSerialAdapter` | `src/infraestructura/ArduinoSerialAdapter.java` |
| 5 | **Observer** *(implementado como mejora)* | Comportamiento | `IStadiumObserver` + `StadiumFacade` | `src/dominio/event/` |

Adicionalmente se documentan dos patrones secundarios que **emergen de forma
natural** del diseno: **DTO / Value Object** (`SensorData`) y **Template Method
implicito** (jerarquias `Sensor` y `Actuator`).

---

## 1. Patron Facade (Estructural)

### Donde vive
- **Clase:** `dominio.StadiumFacade`
- **Archivo:** `Avance/src/dominio/StadiumFacade.java`

### Problema que resuelve
La capa de aplicacion necesita coordinar sensores HC-SR04, actuadores de luces
y alarma, una configuracion de umbrales, un modo de operacion (Strategy) y el
canal de comunicacion con el hardware. Si cada cliente tuviera que conocer a
todos esos objetos, el acoplamiento seria enorme.

### Como esta implementado
`StadiumFacade` agrupa internamente:

- `EntryCounterSensor` y `DistanceSensor` (sensores).
- `LightingZoneActuator` y `AlarmActuator` (actuadores).
- `StadiumConfig` (umbrales).
- `IStadiumModeStrategy` (modo activo).
- `IHardwareComm` (canal con Arduino o simulador).

Expone una **interfaz unificada** mediante metodos como
`readAllSensors()`, `evaluateRules()`, `setAlarm(...)`, `setLight(...)`,
`changeMode(...)`. El controlador (`StadiumController`) nunca toca los sensores
ni el hardware directamente: pasa siempre por la fachada.

### Evidencia en el codigo
- `StadiumController` recibe en su constructor un `StadiumFacade` y todos sus
  handlers (`handleRead`, `handleLight`, `handleAlarm`, `handleMode`, `handleSet`)
  invocan metodos de la fachada.
- `WebDashboardServer` tambien recibe la misma fachada, demostrando que la
  fachada es **la unica puerta de entrada al dominio** para las dos UIs.

### Beneficios concretos
- **Bajo acoplamiento (GRASP):** la presentacion y la aplicacion no conocen a
  los sensores/actuadores individuales.
- **SRP (SOLID):** la fachada se encarga solo de orquestar; los sensores
  almacenan datos, los actuadores manejan estado, el hardware mueve bytes.
- **Proteccion contra variaciones:** se pueden cambiar sensores o actuadores
  internos sin romper a los clientes.

---

## 2. Patron Strategy (Comportamiento)

### Donde vive
- **Interfaz:** `dominio.strategy.IStadiumModeStrategy`
- **Implementaciones:**
  - `AutoModeStrategy`     (HU-05 + HU-07)
  - `ManualModeStrategy`   (HU-09)
  - `EmergencyModeStrategy` (HU-11)
- **Archivos:** `Avance/src/dominio/strategy/`

### Problema que resuelve
El estadio se comporta diferente segun el modo:

- En **AUTO** debe encender luces ante presencia y disparar alarma por
  sobreocupacion automaticamente.
- En **MANUAL** debe respetar exactamente lo que diga el operador.
- En **EMERGENCY** debe forzar alarma y luces al maximo sin importar nada mas.

Implementar esto con `if/else` en la fachada violaria OCP y haria imposible
agregar nuevos modos sin tocar la fachada.

### Como esta implementado
La interfaz define cuatro responsabilidades:

- `getModeName()` para identificacion.
- `evaluate(facade)` para aplicar reglas automaticas y devolver el listado de
  acciones realizadas.
- `canControlLight()` y `canControlAlarm()` para autorizar comandos manuales.

`StadiumFacade.evaluateRules()` delega en `currentMode.evaluate(this)` sin
saber que modo es. `StadiumController.handleMode(...)` instancia la estrategia
correspondiente y llama a `facade.changeMode(...)`.

### Evidencia en el codigo
- En `AutoModeStrategy.evaluate(...)`: lee `f.getDistanceSensor().isPresenceDetected()`,
  enciende o apaga luces, y compara `entryCount` con el umbral para activar
  alarma. Retorna una lista de mensajes describiendo las acciones aplicadas.
- En `ManualModeStrategy.evaluate(...)`: devuelve `Collections.emptyList()` y
  habilita `canControlLight/Alarm = true`.
- En `EmergencyModeStrategy.evaluate(...)`: fuerza alarma y luces a 100% en cada
  ciclo, bloqueando los comandos manuales.
- En `StadiumController.handleLight(...)`: **antes** de cambiar la luz consulta
  `facade.getCurrentMode().canControlLight()`. Si la estrategia dice que no,
  rechaza el comando. Lo mismo para la alarma.

### Beneficios concretos
- **OCP:** agregar un modo nuevo (por ejemplo "NOCTURNO") solo requiere crear
  una clase mas; la fachada y el controlador no cambian.
- **LSP:** cualquier estrategia es intercambiable.
- **Polimorfismo (GRASP):** la fachada llama al modo sin conocer su tipo.

---

## 3. Patron Abstract Factory (Creacional)

### Donde vive
- **Clase abstracta:** `dominio.factory.DeviceFactory`
- **Implementaciones:**
  - `RealHardwareFactory`      -> crea `ArduinoSerialAdapter`
  - `SimulatedHardwareFactory` -> crea `SimulatorAdapter`
- **Archivos:** `Avance/src/dominio/factory/`

### Problema que resuelve
El sistema debe poder correr con **Arduino real** o con **simulador** sin que
el dominio sepa de la diferencia. Si `StadiumFacade` instanciara directamente
`ArduinoSerialAdapter`, dependeria de jSerialComm y seria imposible probar el
sistema sin hardware fisico (HU-14).

### Como esta implementado
`DeviceFactory` declara un unico metodo abstracto: `createHardwareComm()`.
Cada fabrica concreta devuelve la implementacion correspondiente de
`IHardwareComm`:

- `RealHardwareFactory` recibe un nombre de puerto y crea `ArduinoSerialAdapter`.
- `SimulatedHardwareFactory` crea `SimulatorAdapter` sin parametros.

`Main` elige la fabrica segun la opcion del usuario (1 = simulador, 2 =
Arduino) y luego inyecta el resultado en la fachada.

### Evidencia en el codigo
- En `Main.java`: si la opcion es "2", primero se ejecuta
  `ArduinoPortScanner.detect()` para descubrir el puerto, y luego se construye
  `new RealHardwareFactory(port)`. En caso contrario, `new SimulatedHardwareFactory()`.
- La fabrica devuelve el `IHardwareComm`, sobre el cual se llama `connect()` y
  luego se entrega al constructor de `StadiumFacade`. La fachada nunca ve la
  clase concreta.

### Beneficios concretos
- **DIP:** la fachada depende de la abstraccion `IHardwareComm`, no de
  `ArduinoSerialAdapter`.
- **OCP:** anadir un tercer tipo de hardware (por ejemplo BLE o WiFi) consiste
  en crear una fabrica nueva.
- **Pure Fabrication (GRASP):** las fabricas no representan conceptos del
  dominio del estadio; existen unicamente para desacoplar la construccion.

---

## 4. Patron Adapter (Estructural)

### Donde vive
- **Clase:** `infraestructura.ArduinoSerialAdapter`
- **Archivo:** `Avance/src/infraestructura/ArduinoSerialAdapter.java`
- **Libreria adaptada:** `jSerialComm-2.11.0.jar` (en `Avance/lib/`)

### Problema que resuelve
La libreria `jSerialComm` tiene su propia API (`SerialPort.getCommPort`,
`setComPortParameters`, `readBytes`, `writeBytes`, etc.), pero el dominio
exige el contrato `IHardwareComm` con metodos como `readSensors()`,
`setAlarm(...)`, `setLight(...)`. Estos dos lenguajes son incompatibles.

### Como esta implementado
`ArduinoSerialAdapter` **implementa `IHardwareComm`** y dentro encapsula un
objeto `SerialPort` de jSerialComm. Traduce cada metodo del contrato a una
secuencia de operaciones sobre el puerto:

- `connect()` configura velocidad 9600, 8N1, abre el puerto y espera 2 s
  porque el Arduino se reinicia al abrir el serial.
- `readSensors()` manda `READ\n`, lee una linea, valida que comience con
  `DATA:` y delega en `SensorData.parse(...)`.
- `setAlarm(on)` traduce el booleano a `ALARM_ON` o `ALARM_OFF`.
- `setLight(i)` arma el comando `LIGHT_SET:<n>` con saturacion 0-100.
- `setThreshold(cm)` arma el comando `SET_THRESHOLD:<cm>`.

Tambien resuelve detalles propios del transporte fisico: `drain()` para vaciar
el buffer, `readLine()` byte a byte con timeout, y un bloque `synchronized` en
`sendCommand(...)` para evitar carreras entre el dashboard y la consola.

### Evidencia en el codigo
- La declaracion `public class ArduinoSerialAdapter implements IHardwareComm`
  hace explicito el rol de adaptador.
- `SimulatorAdapter` implementa la **misma interfaz** sin usar jSerialComm,
  demostrando que el dominio no esta atado al adaptador real.

### Beneficios concretos
- **Capa anti-corrupcion:** el dominio nunca importa `com.fazecast.jSerialComm`.
- **Testabilidad:** sustituir el adaptador real por el simulador (que tambien
  es una implementacion de `IHardwareComm`) permite probar sin hardware.
- **Aislamiento de cambios:** si en el futuro se reemplaza jSerialComm por
  otra libreria serial, solo cambia esta clase.

---

## 5. Patron Adicional Emergente: DTO / Value Object

### Donde vive
- **Clase:** `dominio.SensorData`
- **Archivo:** `Avance/src/dominio/SensorData.java`

### Caracteristicas
- Todos los campos son `final`: `entryCount`, `distanceCm`, `presenceDetected`,
  `lightIntensity`. Es **inmutable**.
- No tiene comportamiento de negocio; solo expone getters.
- Provee un metodo estatico `parse(response)` que actua como factory method
  para construirlo desde la trama serial `DATA:count,dist,pres,luz`.

Es un **Value Object / DTO** clasico que viaja entre la capa de infraestructura
(donde se construye al leer del puerto) y la capa de dominio (donde se
consume). Su inmutabilidad evita efectos secundarios al pasarlo entre hilos.

---

## 6. Patron Adicional Emergente: Template Method implicito

Las jerarquias `Sensor` (abstracta) -> `DistanceSensor`, `EntryCounterSensor`
y `Actuator` (abstracta) -> `AlarmActuator`, `LightingZoneActuator` definen
estado y comportamiento comun (id, location, active) y dejan a las subclases
las particularidades:

- `DistanceSensor` agrega distancia y un metodo de clasificacion
  (`getDistanceDescription`).
- `LightingZoneActuator` agrega intensidad acotada con `Math.min/max`.

Aunque no hay metodos `abstract` que se sobreescriban en estricto sentido
Template, la estructura es la base sobre la cual aplicar el patron si se
agregaran nuevos tipos de sensor o actuador con un flujo comun de
inicializacion / actualizacion.

---

## 7. Mejora Implementada: Patron Observer (Comportamiento)

Aunque el proyecto ya contaba con cuatro patrones solidos, se identifico una
oportunidad natural de mejora: el firmware Arduino emite eventos `ENTRY:n`
asincronicamente y existen multiples consumidores potenciales del estado del
estadio (consola, dashboard web, archivo de log). Antes de esta mejora, la
fachada solo respondia a consultas bajo demanda y no notificaba cambios.

### Donde vive
- **Paquete nuevo:** `dominio.event`
  - `EventType` (enum con SENSOR_READ, ENTRY_DETECTED, ALARM_CHANGED,
    LIGHT_CHANGED, MODE_CHANGED, THRESHOLD_CHANGED).
  - `StadiumEvent` (evento inmutable: tipo, descripcion, timestamp).
  - `IStadiumObserver` (contrato del observador).
- **Subject:** `dominio.StadiumFacade` ahora mantiene una lista de
  observadores en un `CopyOnWriteArrayList` (seguro para concurrencia) y
  expone `addObserver(...)`, `removeObserver(...)` y un metodo privado
  `notifyObservers(type, description)`.
- **Observadores concretos:**
  - `presentacion.ConsoleEventLogger` imprime eventos en consola.
  - `presentacion.CsvEventLogger` registra eventos en `stadium_events.csv`.

### Como se integra
- En `readAllSensors()` la fachada compara el nuevo `entryCount` contra el
  ultimo conocido y emite un evento `ENTRY_DETECTED` por cada persona nueva.
- `setAlarm`, `setLight`, `setDistanceThreshold` y `changeMode` emiten su
  evento correspondiente solo cuando hay un cambio real respecto al estado
  previo (no se notifica si la operacion no cambia nada).
- `Main.java` registra ambos loggers al iniciar el sistema:
  ```
  facade.addObserver(new ConsoleEventLogger());
  facade.addObserver(new CsvEventLogger("stadium_events.csv"));
  ```
- Al salir del sistema, `csvLogger.close()` cierra el archivo limpiamente.

### Justificacion respecto al proyecto real
- **Aprovecha el evento `ENTRY:n`** que el firmware ya enviaba pero que nadie
  consumia (antes solo se leia el contador acumulado en `DATA:`).
- **Permite agregar nuevas vistas sin tocar la fachada:** una notificacion
  push, un envio a InfluxDB o un panel adicional son nuevas implementaciones
  de `IStadiumObserver`.
- **Tolerancia a fallos:** el `notifyObservers` envuelve cada llamada en
  `try/catch` para que un observador caido no rompa al resto.

### Beneficios SOLID conseguidos
- **OCP:** anadir un observador nuevo no modifica `StadiumFacade`.
- **SRP:** la fachada orquesta, los loggers persisten. Cada uno tiene una sola
  razon de cambio.
- **DIP:** la fachada depende de la interfaz `IStadiumObserver`, no de las
  implementaciones concretas.

---

## Otras oportunidades futuras

### Patron Command para los comandos serial
Actualmente `ArduinoSerialAdapter.sendCommand(...)` arma strings ad-hoc para
cada operacion. Encapsular cada comando (`ReadCommand`, `SetLightCommand`,
`SetAlarmCommand`, `SetThresholdCommand`) como un objeto con metodo
`encode()` permitiria:

- Centralizar el protocolo en un solo lugar.
- Habilitar historial / deshacer (undo) en modo MANUAL.
- Reusar la misma logica de codificacion en pruebas unitarias del simulador.

---

## Conclusion

El proyecto **Avance** implementa **cinco patrones GoF de forma deliberada y
bien integrada**, mas dos emergentes (DTO/Value Object y la base de Template
Method). Cada uno esta justificado por un requisito real:

- **Facade** -> simplificar el acceso al dominio desde dos UIs.
- **Strategy** -> cambiar el comportamiento por modo sin modificar la fachada.
- **Abstract Factory** -> alternar entre Arduino real y simulador.
- **Adapter** -> aislar la libreria jSerialComm del dominio.
- **Observer** *(mejora implementada)* -> notificar cambios a multiples
  suscriptores (consola + CSV) sin acoplar la fachada a ellos.

Esta combinacion da como resultado un sistema con **bajo acoplamiento**, **alta
cohesion** y cumplimiento explicito de los principios **SOLID** (especialmente
OCP, DIP y SRP).
