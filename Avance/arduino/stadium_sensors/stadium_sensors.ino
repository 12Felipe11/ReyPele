// =====================================================================
// ReyPele - Sistema de Control de Estadio Inteligente
// Sensor:    HC-SR04 (ultrasonico)
// Actuador:  DOS tiras WS2812B
//              Tira NORTE: DIN -> pin 6
//              Tira SUR:   DIN -> pin 5
//
// PROTOCOLO SERIAL (9600 baud):
//   Arduino -> Java:  DATA:count,distance,presence,light
//                     ENTRY:totalCount
//
//   Java -> Arduino:
//     READ                             Solicita lectura inmediata
//     ALARM_ON / ALARM_OFF             Activa/desactiva modo emergencia
//     LIGHT_SET:0-100                  Brillo global (ambas tiras en blanco)
//     ZONE_LIGHT:NORTE,0-100           Intensidad tira NORTE (solo modo MANUAL)
//     ZONE_LIGHT:SUR,0-100             Intensidad tira SUR   (solo modo MANUAL)
//     ZONE_COLOR:NORTE,R,G,B           Color tira NORTE
//     ZONE_COLOR:SUR,R,G,B             Color tira SUR
//     SET_THRESHOLD:cm                 Umbral HC-SR04
//     MODE:<X>   (X = OFF|PARTIDO|ENTRENAMIENTO|EVENTO|EMERGENCIA|MANUAL)
// =====================================================================

#include <Adafruit_NeoPixel.h>

// ---- Ajusta pines y cantidades de LEDs segun tu instalacion ----
#define LED_PIN_NORTE  6
#define LED_PIN_SUR    5
#define LEDS_NORTE     8   // cantidad de LEDs en tira Norte
#define LEDS_SUR       8   // cantidad de LEDs en tira Sur
// ----------------------------------------------------------------

Adafruit_NeoPixel stripNORTE(LEDS_NORTE, LED_PIN_NORTE, NEO_GRB + NEO_KHZ800);
Adafruit_NeoPixel stripSUR  (LEDS_SUR,   LED_PIN_SUR,   NEO_GRB + NEO_KHZ800);

// --- HC-SR04 ---
const int TRIG_PIN = 9;
const int ECHO_PIN = 8;

// =====================================================================
// MODOS
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
// ZONAS  (solo las dos tiras fisicas)
// =====================================================================
#define ZONA_NORTE 0
#define ZONA_SUR   1
#define NUM_ZONES  2
const char* ZONE_NAMES[NUM_ZONES] = {"NORTE", "SUR"};

uint32_t zoneColor[NUM_ZONES];      // color RGB puro, sin intensidad aplicada
uint8_t  zoneIntensity[NUM_ZONES];  // 0-100 %

// --- Animaciones no bloqueantes ---
unsigned long lastEventoStep = 0;
const unsigned long EVENTO_STEP_MS = 90;
uint8_t eventoHue = 0;

unsigned long lastAlarmBlink = 0;
const unsigned long ALARM_BLINK_MS = 300;
bool alarmBlinkState = false;

// --- Sensor ---
long  duration;
float distanceCm;
int   entryCount       = 0;
bool  presenciaDetect  = false;
float umbralDistancia  = 30.0;
unsigned long tiempoBloqueo = 0;
const unsigned long BLOQUEO_MS = 800;

// --- Brillo global (promedio de las dos zonas, reportado a Java) ---
int lightIntensity = 0;   // 0-100

// --- Envio periodico ---
unsigned long lastDataSend = 0;
const unsigned long DATA_INTERVAL_MS = 500;

// =====================================================================
void setup() {
  Serial.begin(9600);

  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  stripNORTE.begin();
  stripNORTE.setBrightness(255);
  stripNORTE.clear();
  stripNORTE.show();

  stripSUR.begin();
  stripSUR.setBrightness(255);
  stripSUR.clear();
  stripSUR.show();

  // Inicializar zonas: color blanco, intensidad 0 (apagadas)
  for (uint8_t i = 0; i < NUM_ZONES; i++) {
    zoneColor[i]     = Adafruit_NeoPixel::Color(255, 255, 255);
    zoneIntensity[i] = 0;
  }

  Serial.println("READY");
}

// =====================================================================
void loop() {
  processSerialCommands();

  // HC-SR04
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  duration   = pulseIn(ECHO_PIN, HIGH, 30000);
  distanceCm = (duration == 0) ? 999.0 : duration * 0.0343 / 2.0;

  // Conteo por flanco
  if (!presenciaDetect && distanceCm < umbralDistancia) {
    presenciaDetect = true;
  } else if (presenciaDetect && distanceCm >= umbralDistancia) {
    if (millis() - tiempoBloqueo > BLOQUEO_MS) {
      entryCount++;
      tiempoBloqueo = millis();
      Serial.print("ENTRY:");
      Serial.println(entryCount);
    }
    presenciaDetect = false;
  }

  // Envio periodico
  if (millis() - lastDataSend >= DATA_INTERVAL_MS) {
    sendData();
    lastDataSend = millis();
  }

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

    if (cmd == "READ") {
      sendData();

    } else if (cmd == "ALARM_ON") {
      currentMode = MODE_EMERGENCIA;

    } else if (cmd == "ALARM_OFF") {
      if (currentMode == MODE_EMERGENCIA) currentMode = MODE_OFF;

    } else if (cmd.startsWith("LIGHT_SET:")) {
      // Brillo global: ambas tiras al mismo nivel en blanco
      lightIntensity = constrain(cmd.substring(10).toInt(), 0, 100);
      if (currentMode == MODE_MANUAL) {
        zoneIntensity[ZONA_NORTE] = (uint8_t)lightIntensity;
        zoneIntensity[ZONA_SUR]   = (uint8_t)lightIntensity;
        zoneColor[ZONA_NORTE] = Adafruit_NeoPixel::Color(255, 255, 255);
        zoneColor[ZONA_SUR]   = Adafruit_NeoPixel::Color(255, 255, 255);
      }
      Serial.println("OK:LIGHT");

    } else if (cmd.startsWith("ZONE_LIGHT:")) {
      // ZONE_LIGHT:NORTE,80  o  ZONE_LIGHT:SUR,50
      // Activa modo MANUAL automaticamente (excepto en emergencia).
      String params = cmd.substring(11);
      int comma = params.indexOf(',');
      if (comma > 0) {
        String  zoneName  = params.substring(0, comma);
        uint8_t intensity = (uint8_t)constrain(params.substring(comma + 1).toInt(), 0, 100);
        int idx = findZone(zoneName);
        if (idx >= 0) {
          zoneIntensity[idx] = intensity;
          if (currentMode != MODE_EMERGENCIA) currentMode = MODE_MANUAL;
          Serial.println("OK:ZONE_LIGHT");
        } else {
          Serial.println("ERROR:ZONE_NOT_FOUND");
        }
      }

    } else if (cmd.startsWith("ZONE_COLOR:")) {
      // ZONE_COLOR:NORTE,255,0,0
      String params = cmd.substring(11);
      int c1 = params.indexOf(',');
      int c2 = (c1 >= 0) ? params.indexOf(',', c1 + 1) : -1;
      int c3 = (c2 >= 0) ? params.indexOf(',', c2 + 1) : -1;
      if (c1 > 0 && c2 > 0 && c3 > 0) {
        String  zoneName = params.substring(0, c1);
        uint8_t r = (uint8_t)constrain(params.substring(c1 + 1, c2).toInt(), 0, 255);
        uint8_t g = (uint8_t)constrain(params.substring(c2 + 1, c3).toInt(), 0, 255);
        uint8_t b = (uint8_t)constrain(params.substring(c3 + 1).toInt(),     0, 255);
        int idx = findZone(zoneName);
        if (idx >= 0) {
          zoneColor[idx] = Adafruit_NeoPixel::Color(r, g, b);
          if (currentMode != MODE_EMERGENCIA) currentMode = MODE_MANUAL;
          Serial.println("OK:ZONE_COLOR");
        } else {
          Serial.println("ERROR:ZONE_NOT_FOUND");
        }
      }

    } else if (cmd.startsWith("SET_THRESHOLD:")) {
      umbralDistancia = cmd.substring(14).toFloat();

    } else if (cmd.startsWith("MODE:")) {
      applyModeCommand(cmd.substring(5));
    }
  }
}

void applyModeCommand(const String& m) {
  if      (m == "OFF")           currentMode = MODE_OFF;
  else if (m == "PARTIDO")       currentMode = MODE_PARTIDO;
  else if (m == "ENTRENAMIENTO") currentMode = MODE_ENTRENAMIENTO;
  else if (m == "EVENTO")      { currentMode = MODE_EVENTO; eventoHue = 0; }
  else if (m == "EMERGENCIA")    currentMode = MODE_EMERGENCIA;
  else if (m == "MANUAL")        currentMode = MODE_MANUAL;
}

int findZone(const String& name) {
  for (uint8_t i = 0; i < NUM_ZONES; i++) {
    if (name.equalsIgnoreCase(ZONE_NAMES[i])) return i;
  }
  return -1;
}

// =====================================================================
void sendData() {
  // Intensidad global = promedio de las dos zonas
  lightIntensity = ((int)zoneIntensity[ZONA_NORTE] + (int)zoneIntensity[ZONA_SUR]) / 2;

  Serial.print("DATA:");
  Serial.print(entryCount);
  Serial.print(",");
  Serial.print(distanceCm, 1);
  Serial.print(",");
  Serial.print(presenciaDetect ? 1 : 0);
  Serial.print(",");
  Serial.println(lightIntensity);
}

// =====================================================================
// HELPERS DE TIRA
// =====================================================================

void fillStrip(Adafruit_NeoPixel& s, uint32_t color) {
  for (int i = 0; i < s.numPixels(); i++) s.setPixelColor(i, color);
}

void fillBoth(uint32_t color) {
  fillStrip(stripNORTE, color);
  fillStrip(stripSUR,   color);
}

// Aplica el color e intensidad almacenados de una zona a su tira fisica
void applyZone(uint8_t idx) {
  Adafruit_NeoPixel& s = (idx == ZONA_NORTE) ? stripNORTE : stripSUR;
  uint8_t pct = zoneIntensity[idx];
  if (pct == 0) { fillStrip(s, 0); return; }
  uint32_t c = zoneColor[idx];
  uint8_t r = (uint8_t)(((c >> 16) & 0xFF) * pct / 100);
  uint8_t g = (uint8_t)(((c >>  8) & 0xFF) * pct / 100);
  uint8_t b = (uint8_t)(( c        & 0xFF) * pct / 100);
  fillStrip(s, Adafruit_NeoPixel::Color(r, g, b));
}

// =====================================================================
// ACTUALIZACION DE LEDS
// =====================================================================
void updateLeds() {
  stripNORTE.setBrightness(255);
  stripSUR.setBrightness(255);

  switch (currentMode) {
    case MODE_MANUAL:        renderManual();        break;
    case MODE_PARTIDO:       renderPartido();       break;
    case MODE_ENTRENAMIENTO: renderEntrenamiento(); break;
    case MODE_EVENTO:        renderEvento();        break;
    case MODE_EMERGENCIA:    renderEmergencia();    break;
    default:                 fillBoth(0);           break;  // MODE_OFF
  }

  stripNORTE.show();
  stripSUR.show();
}

// MANUAL: cada tira con su propia intensidad y color
void renderManual() {
  applyZone(ZONA_NORTE);
  applyZone(ZONA_SUR);
}

// PARTIDO: ambas tiras en blanco calido
void renderPartido() {
  uint8_t b = (uint8_t)map(lightIntensity, 0, 100, 0, 255);
  if (b == 0) b = 180;
  stripNORTE.setBrightness(b);
  stripSUR.setBrightness(b);
  fillBoth(Adafruit_NeoPixel::Color(255, 220, 160));
}

// ENTRENAMIENTO: ambas tiras en blanco frio
void renderEntrenamiento() {
  uint8_t b = (uint8_t)map(lightIntensity, 0, 100, 0, 255);
  if (b == 0) b = 180;
  stripNORTE.setBrightness(b);
  stripSUR.setBrightness(b);
  fillBoth(Adafruit_NeoPixel::Color(255, 255, 220));
}

// EVENTO: ola de colores sincronizada en ambas tiras
void renderEvento() {
  if (millis() - lastEventoStep >= EVENTO_STEP_MS) {
    lastEventoStep = millis();
    eventoHue = (eventoHue + 3) % 255;
  }
  for (int i = 0; i < LEDS_NORTE; i++) {
    uint8_t phase = (uint8_t)((i * 255 / LEDS_NORTE + eventoHue) % 255);
    uint32_t c;
    if      (phase < 85)  c = Adafruit_NeoPixel::Color(0,   255, 0);
    else if (phase < 170) c = Adafruit_NeoPixel::Color(255, 200, 0);
    else                  c = Adafruit_NeoPixel::Color(255, 0,   0);
    stripNORTE.setPixelColor(i, c);
  }
  for (int i = 0; i < LEDS_SUR; i++) {
    uint8_t phase = (uint8_t)((i * 255 / LEDS_SUR + eventoHue) % 255);
    uint32_t c;
    if      (phase < 85)  c = Adafruit_NeoPixel::Color(0,   255, 0);
    else if (phase < 170) c = Adafruit_NeoPixel::Color(255, 200, 0);
    else                  c = Adafruit_NeoPixel::Color(255, 0,   0);
    stripSUR.setPixelColor(i, c);
  }
}

// EMERGENCIA: parpadeo rojo rapido en ambas tiras
void renderEmergencia() {
  if (millis() - lastAlarmBlink >= ALARM_BLINK_MS) {
    lastAlarmBlink  = millis();
    alarmBlinkState = !alarmBlinkState;
  }
  fillBoth(alarmBlinkState ? Adafruit_NeoPixel::Color(255, 0, 0) : 0);
}
