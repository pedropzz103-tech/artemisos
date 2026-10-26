# IR Capture | VISIONE Lab
Android app to capture Mi Remote infrared commands with an ESP32 receiver and export Flipper Zero-style .ir files.

## Build
GitHub Actions workflow **IR Capture Android APK** builds a debug APK and uploads it as **IRCapture-debug-apk**. Download the artifact ZIP from the workflow run, unzip it, then install **app-debug.apk**.

## Hardware
Xiaomi with IR transmitter; ESP32 DevKit V1; VS1838B/TSOP38238 receiver connected OUT to GPIO27, VCC to 3V3, GND to GND. Verify physical pin order before connecting.

## ESP32 firmware
Open esp32/IRCapture_ESP32/IRCapture_ESP32.ino in Arduino IDE; install IRremoteESP8266; upload to ESP32. ESP32 AP: **IR-CAPTURE**, password **irlearn123**, address **192.168.4.1**.

## Capture
Connect phone to ESP32 Wi-Fi AP; launch IR Capture; tap CAPTURAR PRÓXIMO SINAL; aim the Xiaomi IR emitter at the external IR receiver while issuing a button in Mi Remote; repeat and export to a single .ir file.

Note: Xiaomi's built-in IR emitter alone cannot receive IR. Default carrier (38kHz) is assumed, not measured. Captures are held in RAM until exported. Source is a debug build and is not signed for app-store deployment.
