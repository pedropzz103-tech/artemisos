package one.visione.caravanos;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.json.JSONObject;

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
  s.setAllowUniversalAccessFromFileURLs(false);
  web.addJavascriptInterface(new Bridge(), "CaravanBridge");
  setContentView(web);
  web.loadUrl("file:///android_asset/index.html");
 }
 private class Bridge {
  @JavascriptInterface public void request(String path, String method, int id) {
   new Thread(() -> {
    String result;
    try {
     if (!path.matches("/api/state|/api/alarm|/api/light\\?id=[0-4]") || !(method.equals("GET") || method.equals("POST"))) throw new Exception("Invalid request");
     HttpURLConnection c=(HttpURLConnection)new URL("http://192.168.4.1"+path).openConnection();
     c.setRequestMethod(method); c.setConnectTimeout(1800); c.setReadTimeout(1800);
     if(c.getResponseCode()!=200) throw new Exception("HTTP "+c.getResponseCode());
     try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buffer=new byte[1024];int n;
      while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
      result=new String(out.toByteArray(),"UTF-8");
     }finally{c.disconnect();}
     new JSONObject(result);
     final String payload=result;
     runOnUiThread(()->web.evaluateJavascript("window.caravanReply("+id+",true,"+JSONObject.quote(payload)+")",null));
    }catch(Exception e){
     runOnUiThread(()->web.evaluateJavascript("window.caravanReply("+id+",false,'')",null));
    }
   }).start();
  }
 }
 @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
