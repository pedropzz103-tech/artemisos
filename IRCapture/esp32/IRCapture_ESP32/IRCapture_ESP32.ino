/*
 * IR Capture • ESP32 DevKit V1
 * IR Receiver (VS1838B / TSOP38238): OUT -> GPIO27, GND -> GND,
 * VCC -> 3V3 (check the module's datasheet).
 * Install library "IRremoteESP8266" by crankyoldgit in Arduino IDE.
 * Creates Wi-Fi AP IR-CAPTURE / irlearn123 at http://192.168.4.1
 * Captures raw pulses for .ir export by the Android app.
 */
#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <IRremoteESP8266.h>
#include <IRrecv.h>

static const uint16_t kPin = 27;
static const uint16_t kBufferSize = 2048;
static const uint8_t kTimeout = 50;
static const uint16_t kMaxFlipperSamples = 1024;
static const uint32_t kFrequency = 38000; // typical carrier, not measured

IRrecv receiver(kPin, kBufferSize, kTimeout, true);
decode_results decoded;
WebServer server(80);

uint32_t captureId = 0;
uint32_t lastSamples[kMaxFlipperSamples];
uint16_t lastLength = 0;
bool lastOverflow = false;
bool armed = false;

void armCapture() {
  armed = true;
  lastLength = 0;
  lastOverflow = false;
  receiver.resume();
  server.sendHeader("Cache-Control", "no-store");
  server.send(200, "application/json", String("{\"id\":") + captureId + ",\"armed\":true}");
}

void latest() {
  String json;
  json.reserve(8000);
  json = String("{\"id\":") + captureId +
    ",\"armed\":" + (armed ? "true" : "false") +
    ",\"overflow\":" + (lastOverflow ? "true" : "false") +
    ",\"frequency\":" + kFrequency +
    ",\"length\":" + lastLength + ",\"data\":[";
  for (uint16_t i=0; i<lastLength; i++) {
    if (i) json += ',';
    json += lastSamples[i];
  }
  json += "]}";
  server.sendHeader("Cache-Control", "no-store");
  server.send(200, "application/json", json);
}

void setup() {
  Serial.begin(115200);
  delay(250);
  receiver.enableIRIn();
  WiFi.mode(WIFI_AP);
  WiFi.softAP("IR-CAPTURE", "irlearn123");
  Serial.println("\nIR Capture pronto");
  Serial.print("IP: ");
  Serial.println(WiFi.softAPIP());
  server.on("/api/arm", HTTP_GET, armCapture);
  server.on("/api/latest", HTTP_GET, latest);
  server.on("/", HTTP_GET, [](){
    server.send(200,"text/plain", "IR Capture ESP32 OK. Use /api/arm then /api/latest");
  });
  server.onNotFound([](){ server.send(404,"text/plain","Not found"); });
  server.begin();
}

void loop() {
  server.handleClient();
  if (receiver.decode(&decoded)) {
    if (armed) {
      uint16_t length = decoded.rawlen > 0 ? decoded.rawlen - 1 : 0;
      if (length >= 10) {
        lastOverflow = decoded.overflow || length > kMaxFlipperSamples;
        lastLength = lastOverflow ? 0 : length;
        for (uint16_t i=0; i<lastLength; i++) {
          lastSamples[i] = (uint32_t)decoded.rawbuf[i+1] * kRawTick;
        }
        captureId++;
        armed = false;
        Serial.printf("Captured %u timings; overflow=%d; id=%lu\n",
          length, lastOverflow, (unsigned long)captureId);
      }
    }
    receiver.resume();
  }
  delay(1);
}
