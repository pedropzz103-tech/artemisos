# VISIONE IR Blaster (Android / Xiaomi)

Native Android app that uses the phone's **hardware infrared emitter** (Android `ConsumerIrManager`) to send test TV power codes and import remote files in **Flipper Zero `.ir` format**.

No ESP32 needed for *transmitting* IR. A receiver (e.g. ESP32 + VS1838B) is still required to *capture* unknown commands from a remote. This app **does not** claim to capture IR via the phone's transmitter.

## Features
- Samsung TV: one common Samsung32 power command (0xE0E040BF).
- LG TV: one common NEC-style power command (0x20DF10EF).
- Sony TV: one common SIRC12 power command (0xA90).
- Manual testing, plus an explicitly confirmed sequence of three common TV codes.
- Import and store a Flipper Zero `.ir` file from device storage. Sends RAW and parsed NEC, NECext, Samsung32, SIRC, SIRC15, SIRC20 signals.
- Filter imported signals by their saved button names.

**Cautions:** TV Power codes are often toggles; the device might turn *on* instead of *off*. Use only on your own devices or with permission. AC controls often require long stateful RAW frames. The specific Kosner/Midea profile is not known and **must be captured** or imported from a reliable source before it can be faithfully transmitted.

## Build
The GitHub Actions workflow **Build IR Blaster APK** runs Gradle 8.9 and Android SDK 35, and uploads an artifact called `VISIONE-IRBlaster-APK` containing `app-debug.apk`. It is a signed debug/test APK, not a Play Store release.

## Compatibility
Android 7.0+ with a functioning IR transmitter available through `ConsumerIrManager` (as on some Xiaomi / Redmi phones). Some MIUI/HyperOS devices may restrict public access to their emitter. The app checks and reports whether the interface exists.

## Data
Imported remote definitions remain in the app's private storage on the phone. No network permission, analytics, or account access.
