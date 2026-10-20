package one.visione.caravanos;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
  web = new WebView(this);
  web.setWebViewClient(new WebViewClient());
  WebSettings s = web.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setAllowFileAccess(true);
  s.setAllowFileAccessFromFileURLs(false);
  s.setAllowUniversalAccessFromFileURLs(true); // Trusted bundled page only; prototype local ESP32 HTTP
  setContentView(web);
  web.loadUrl("file:///android_asset/index.html");
 }
 @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
