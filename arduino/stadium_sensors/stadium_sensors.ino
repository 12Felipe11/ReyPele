// =====================================================================
// ReyPele - Sistema de Control de Estadio Inteligente
// Sensor:    HC-SR04 (ultrasonico)
// Pantalla:  OLED SSD1306 128x64
// Actuador:  Tira WS2812B-8 (8 LEDs direccionables, DIN en pin 6)
//
// PROTOCOLO SERIAL (9600 baud):
//   Arduino -> Java (cada 500ms):  DATA:count,distance,presence,light
//   Arduino -> Java (por evento):  ENTRY:totalCount
//
//   Java -> Arduino (comandos):
//     ALARM_ON / ALARM_OFF             (compatibilidad; mapea a MODE:EMERGENCIA/OFF)
//     LIGHT_SET:0-100                  Brillo global de la tira
//     SET_THRESHOLD:cm                 Umbral de presencia HC-SR04
//     READ                             Solicita lectura DATA: inmediata
//     MODE:<X>                         X = OFF | PARTIDO | ENTRENAMIENTO |
//                                            EVENTO | EMERGENCIA | MANUAL
//     ZONE:<NOMBRE>:<R>,<G>,<B>        Color RGB para una zona (entra MANUAL)
//     ZONE:<NOMBRE>:OFF                Apaga la zona (entra MANUAL)
//        NOMBRE: NORTE | ORIENTAL | CANCHA | OCCIDENTAL | SUR
// =====================================================================

#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <Adafruit_NeoPixel.h>

// --- Pantalla OLED ---
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET    -1
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// --- Pines HC-SR04 ---
const int TRIG_PIN = 9;
const int ECHO_PIN = 8;

// --- Tira WS2812B-8 (8 LEDs direccionables) ---
// DIN del WS2812B al pin 6 (con resistencia ~330 ohm en serie).
// Alimentar la tira a 5 V con fuente externa y GND comun con el Arduino.
#define LED_PIN     6
#define NUM_LEDS    8
Adafruit_NeoPixel strip(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

// =====================================================================
// MODOS DEL ESTADIO
// =====================================================================
enum Mode {
  MODE_OFF,
  MODE_PARTIDO,
  MODE_ENTRENAMIENTO,
  MODE_EVENTO,
  MODE_EMERGENCIA,
  MODE_MANUAL
};
Mode currentMode = MODE_OFF;

// =====================================================================
// ZONAS DEL ESTADIO (5 zonas mapeadas sobre los 8 LEDs)
//   LED:    0     1 2     3 4     5 6     7
//   Zona: NORTE | ORI    CANCHA | OCC   | SUR
// =====================================================================
struct Zone {
  const char* name;
  uint8_t startLed;
  uint8_t count;
};
const Zone ZONES[] = {
  {"NORTE",      0, 1},
  {"ORIENTAL",   1, 2},
  {"CANCHA",     3, 2},
  {"OCCIDENTAL", 5, 2},
  {"SUR",        7, 1}
};
const uint8_t NUM_ZONES = sizeof(ZONES) / sizeof(ZONES[0]);

// Color por zona en MODE_MANUAL
uint32_t zoneManualColor[NUM_ZONES] = {0, 0, 0, 0, 0};

// --- Animaciones (no bloqueantes) ---
unsigned long lastEventoStep = 0;
const unsigned long EVENTO_STEP_MS = 90;
uint8_t eventoPos = 0;

unsigned long lastAlarmBlink = 0;
const unsigned long ALARM_BLINK_MS = 300;
bool alarmBlinkState = false;

// --- Variables sensor ---
long duration;
float distanceCm;

// --- Logica de conteo ---
int entryCount = 0;
bool presenciaDetectada = false;
float umbralDistancia = 30.0;       // cm para considerar "paso"
unsigned long tiempoBloqueo = 0;
const unsigned long BLOQUEO_MS = 800;

// --- Brillo global ---
int lightIntensity = 0;             // 0-100, controlado desde Java

// --- Envio periodico de datos ---
unsigned long lastDataSend = 0;
const unsigned long DATA_INTERVAL_MS = 500;

// =====================================================================
void setup() {
  Serial.begin(9600);

  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    pinMode(LED_BUILTIN, OUTPUT);
    while (true) {
      digitalWrite(LED_BUILTIN, HIGH); delay(200);
      digitalWrite(LED_BUILTIN, LOW);  delay(200);
    }
  }
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0, 0);
  display.println("ReyPele Stadium");
  display.println("Iniciando...");
  display.display();
  delay(2000);

  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  strip.begin();
  strip.setBrightness(0);
  strip.clear();
  strip.show();

  Serial.println("READY");
}

// =====================================================================
void loop() {
  processSerialCommands();

  // Medir distancia HC-SR04
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  duration = pulseIn(ECHO_PIN, HIGH, 30000);
  distanceCm = (duration == 0) ? 999.0 : duration * 0.0343 / 2.0;

  // Conteo por flanco (acerca + aleja)
  if (!presenciaDetectada && distanceCm < umbralDistancia) {
    presenciaDetectada = true;
  } else if (presenciaDetectada && distanceCm >= umbralDistancia) {
    if (millis() - tiempoBloqueo > BLOQUEO_MS) {
      entryCount++;
      tiempoBloqueo = millis();
      Serial.print("ENTRY:");
      Serial.println(entryCount);
    }
    presenciaDetectada = false;
  }

  // Envio periodico a Java
  if (millis() - lastDataSend >= DATA_INTERVAL_MS) {
    sendData();
    lastDataSend = millis();
  }

  updateDisplay();
  updateLeds();

  delay(50);
}

// =====================================================================
// COMANDOS SERIAL
// =====================================================================
void processSerialCommands() {
  while (Serial.available() > 0) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd.length() == 0) continue;

    if (cmd == "ALARM_ON") {
      currentMode = MODE_EMERGENCIA;
    } else if (cmd == "ALARM_OFF") {
      if (currentMode == MODE_EMERGENCIA) currentMode = MODE_OFF;
    } else if (cmd.startsWith("LIGHT_SET:")) {
      lightIntensity = constrain(cmd.substring(10).toInt(), 0, 100);
    } else if (cmd.startsWith("SET_THRESHOLD:")) {
      umbralDistancia = cmd.substring(14).toFloat();
    } else if (cmd == "READ") {
      sendData();
    } else if (cmd.startsWith("MODE:")) {
      applyModeCommand(cmd.substring(5));
    } else if (cmd.startsWith("ZONE:")) {
      applyZoneCommand(cmd.substring(5));
    }
  }
}

void applyModeCommand(const String& m) {
  if      (m == "OFF")           currentMode = MODE_OFF;
  else if (m == "PARTIDO")       currentMode = MODE_PARTIDO;
  else if (m == "ENTRENAMIENTO") currentMode = MODE_ENTRENAMIENTO;
  else if (m == "EVENTO")      { currentMode = MODE_EVENTO; eventoPos = 0; }
  else if (m == "EMERGENCIA")    currentMode = MODE_EMERGENCIA;
  else if (m == "MANUAL")        currentMode = MODE_MANUAL;
}

void applyZoneCommand(const String& payload) {
  // payload: "<NAME>:<R>,<G>,<B>"  o  "<NAME>:OFF"
  int colon = payload.indexOf(':');
  if (colon <= 0) return;
  String name = payload.substring(0, colon);
  String rest = payload.substring(colon + 1);

  int idx = findZone(name);
  if (idx < 0) return;

  currentMode = MODE_MANUAL;
  if (rest == "OFF") {
    zoneManualColor[idx] = 0;
    return;
  }
  int c1 = rest.indexOf(',');
  int c2 = rest.indexOf(',', c1 + 1);
  if (c1 <= 0 || c2 <= 0) return;
  int r = constrain(rest.substring(0, c1).toInt(), 0, 255);
  int g = constrain(rest.substring(c1 + 1, c2).toInt(), 0, 255);
  int b = constrain(rest.substring(c2 + 1).toInt(), 0, 255);
  zoneManualColor[idx] = strip.Color(r, g, b);
}

int findZone(const String& name) {
  for (uint8_t i = 0; i < NUM_ZONES; i++) {
    if (name.equals(ZONES[i].name)) return i;
  }
  return -1;
}

// DATA:entradas,distancia,presencia(0|1),luzIntensidad
void sendData() {
  Serial.print("DATA:");
  Serial.print(entryCount);
  Serial.print(",");
  Serial.print(distanceCm, 1);
  Serial.print(",");
  Serial.print(presenciaDetectada ? 1 : 0);
  Serial.print(",");
  Serial.println(lightIntensity);
}

// =====================================================================
// PANTALLA OLED
// =====================================================================
const char* modeLabel() {
  switch (currentMode) {
    case MODE_OFF:           return "OFF";
    case MODE_PARTIDO:       return "PART";
    case MODE_ENTRENAMIENTO: return "ENT";
    case MODE_EVENTO:        return "EVNT";
    case MODE_EMERGENCIA:    return "EMER";
    case MODE_MANUAL:        return "MAN";
  }
  return "?";
}

void updateDisplay() {
  display.clearDisplay();

  display.setTextSize(1);
  display.setCursor(0, 0);
  display.println("=== REYPELE ===");

  display.print("Dist: ");
  if (distanceCm >= 400) display.println("---  cm");
  else { display.print(distanceCm, 1); display.println(" cm"); }

  display.setTextSize(2);
  display.setCursor(0, 24);
  display.print("Ent:");
  display.println(entryCount);

  display.setTextSize(1);
  display.setCursor(0, 48);
  display.print("[");
  display.print(modeLabel());
  display.print("] L:");
  display.print(lightIntensity);
  display.print("%");

  if (presenciaDetectada) {
    display.setCursor(110, 0);
    display.print("*");
  }
  display.display();
}

// =====================================================================
// CONTROL WS2812B-8: render por modo + brillo global
// =====================================================================
void updateLeds() {
  // Brillo global (LIGHT_SET). Emergencia se fuerza visible.
  uint8_t b = map(lightIntensity, 0, 100, 0, 255);
  if (currentMode == MODE_EMERGENCIA && b < 180) b = 255;
  if (currentMode != MODE_OFF && b == 0) b = 180;  // visible aunque no haya LIGHT_SET
  strip.setBrightness(b);

  switch (currentMode) {
    case MODE_OFF:           strip.clear(); break;
    case MODE_PARTIDO:       renderPartido(); break;
    case MODE_ENTRENAMIENTO: renderEntrenamiento(); break;
    case MODE_EVENTO:        renderEvento(); break;
    case MODE_EMERGENCIA:    renderEmergencia(); break;
    case MODE_MANUAL:        renderManual(); break;
  }
  strip.show();
}

void fillZone(uint8_t idx, uint32_t color) {
  for (uint8_t i = 0; i < ZONES[idx].count; i++) {
    strip.setPixelColor(ZONES[idx].startLed + i, color);
  }
}

// PARTIDO: estadio completo en blanco calido
void renderPartido() {
  uint32_t blancoCalido = strip.Color(255, 220, 160);
  for (uint8_t i = 0; i < NUM_ZONES; i++) fillZone(i, blancoCalido);
}

// ENTRENAMIENTO: solo cancha
void renderEntrenamiento() {
  uint32_t blanco = strip.Color(255, 255, 220);
  for (uint8_t i = 0; i < NUM_ZONES; i++) {
    bool esCancha = (ZONES[i].name[0] == 'C');
    fillZone(i, esCancha ? blanco : 0);
  }
}

// EVENTO: ola Verde -> Amarillo -> Rojo barriendo el estadio
void renderEvento() {
  if (millis() - lastEventoStep >= EVENTO_STEP_MS) {
    lastEventoStep = millis();
    eventoPos = (eventoPos + 1) % NUM_LEDS;
  }
  for (uint8_t i = 0; i < NUM_LEDS; i++) {
    uint8_t phase = (i + eventoPos) % NUM_LEDS;
    uint32_t c;
    if (phase < NUM_LEDS / 3)            c = strip.Color(0,   255, 0);     // verde
    else if (phase < (2 * NUM_LEDS) / 3) c = strip.Color(255, 200, 0);     // amarillo
    else                                 c = strip.Color(255, 0,   0);     // rojo
    strip.setPixelColor(i, c);
  }
}

// EMERGENCIA: parpadeo rojo en todos los LEDs
void renderEmergencia() {
  if (millis() - lastAlarmBlink >= ALARM_BLINK_MS) {
    lastAlarmBlink = millis();
    alarmBlinkState = !alarmBlinkState;
  }
  uint32_t c = alarmBlinkState ? strip.Color(255, 0, 0) : 0;
  for (uint8_t i = 0; i < NUM_LEDS; i++) strip.setPixelColor(i, c);
}

// MANUAL: cada zona con el color que Java le haya asignado
void renderManual() {
  for (uint8_t i = 0; i < NUM_ZONES; i++) fillZone(i, zoneManualColor[i]);
}
