// =====================================================================
// ReyPele - Sistema de Control de Estadio Inteligente
// Sensor: HC-SR04 (ultrasonico) | Pantalla: OLED SSD1306 128x64
//
// PROTOCOLO SERIAL (9600 baud):
//   Arduino -> Java (cada 500ms):  DATA:count,distance,presence,light
//   Arduino -> Java (por evento):  ENTRY:totalCount
//   Java -> Arduino (comandos):    ALARM_ON / ALARM_OFF
//                                  LIGHT_SET:0-100
//                                  SET_THRESHOLD:cm
//                                  READ
// =====================================================================

#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// --- Pantalla OLED ---
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET    -1
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// --- Pines HC-SR04 ---
const int TRIG_PIN = 9;
const int ECHO_PIN = 8;

// --- Variables sensor ---
long duration;
float distanceCm;

// --- Logica de conteo ---
int entryCount = 0;
bool presenciaDetectada = false;
float umbralDistancia = 30.0;       // cm para considerar "paso"
unsigned long tiempoBloqueo = 0;
const unsigned long BLOQUEO_MS = 800;

// --- Estados de actuadores (recibidos desde Java) ---
bool alarmActive = false;
int lightIntensity = 0;

// --- Envio periodico de datos ---
unsigned long lastDataSend = 0;
const unsigned long DATA_INTERVAL_MS = 500;

// =====================================================================
void setup() {
  Serial.begin(9600);

  if (!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    // Si falla la pantalla, parpadear con el LED integrado
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

  Serial.println("READY");  // Senial de inicio para Java
}

// =====================================================================
void loop() {
  // 1. Procesar comandos entrantes desde Java
  processSerialCommands();

  // 2. Medir distancia con HC-SR04
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  // timeout 30000us = rango max ~5m; si no hay eco = fuera de rango
  duration = pulseIn(ECHO_PIN, HIGH, 30000);
  distanceCm = (duration == 0) ? 999.0 : duration * 0.0343 / 2.0;

  // 3. Logica de conteo (entrada completada cuando se aleja tras acercarse)
  if (!presenciaDetectada && distanceCm < umbralDistancia) {
    presenciaDetectada = true;
  } else if (presenciaDetectada && distanceCm >= umbralDistancia) {
    if (millis() - tiempoBloqueo > BLOQUEO_MS) {
      entryCount++;
      tiempoBloqueo = millis();
      // Evento inmediato de nueva entrada
      Serial.print("ENTRY:");
      Serial.println(entryCount);
    }
    presenciaDetectada = false;
  }

  // 4. Envio periodico de datos al sistema Java
  if (millis() - lastDataSend >= DATA_INTERVAL_MS) {
    sendData();
    lastDataSend = millis();
  }

  // 5. Actualizar pantalla OLED
  updateDisplay();

  delay(100);
}

// =====================================================================
void processSerialCommands() {
  while (Serial.available() > 0) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();

    if (cmd == "ALARM_ON") {
      alarmActive = true;
    } else if (cmd == "ALARM_OFF") {
      alarmActive = false;
    } else if (cmd.startsWith("LIGHT_SET:")) {
      lightIntensity = constrain(cmd.substring(10).toInt(), 0, 100);
    } else if (cmd.startsWith("SET_THRESHOLD:")) {
      umbralDistancia = cmd.substring(14).toFloat();
    } else if (cmd == "READ") {
      sendData();   // Respuesta inmediata a solicitud READ
    }
  }
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

void updateDisplay() {
  display.clearDisplay();

  // Linea 1: titulo
  display.setTextSize(1);
  display.setCursor(0, 0);
  display.println("=== REYPELE ===");

  // Linea 2: distancia
  display.print("Dist: ");
  if (distanceCm >= 400) {
    display.println("---  cm");
  } else {
    display.print(distanceCm, 1);
    display.println(" cm");
  }

  // Linea 3-4: contador grande
  display.setTextSize(2);
  display.setCursor(0, 24);
  display.print("Ent:");
  display.println(entryCount);

  // Linea 5: estado actuadores
  display.setTextSize(1);
  display.setCursor(0, 48);
  display.print(alarmActive ? "[ALM:ON]" : "[ALM:--]");
  display.print(" L:");
  display.print(lightIntensity);
  display.print("%");

  // Indicador de presencia
  if (presenciaDetectada) {
    display.setCursor(110, 0);
    display.print("*");
  }

  display.display();
}
