# Caravan OS Tablet (Android 10)
Native Android Activity with locally bundled dashboard, kiosk-style fullscreen landscape, and direct ESP32 API requests to http://192.168.4.1.

## Build
Run the GitHub Actions workflow `Caravan OS Tablet APK`, then download artifact `CaravanOS-Android10-debug` and install its APK on the Alcatel Android 10 tablet. Connect tablet to Wi-Fi SSID `CaravanOS` first.

## Safety / limitations
Light and alarm states are currently simulated by the ESP32 firmware. No physical load is switched. ESP32 HTTP API is unauthenticated; use only on a trusted isolated test AP. This initial Android version uses targetSdk 28 to permit cleartext HTTP to the ESP32 and a file-based WebView dashboard; before production, modernize security and network transport. If cross-origin requests are blocked by WebView, replace file:// requests with a native Java bridge or use WebViewAssetLoader. APK compilation and device testing are not yet verified.
