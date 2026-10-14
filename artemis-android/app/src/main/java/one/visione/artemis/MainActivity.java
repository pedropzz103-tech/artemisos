package one.visione.artemis;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
 private TextToSpeech tts; private SpeechRecognizer recognizer; private TextView status; private Orb orb;
 private boolean ready = false;
 @Override public void onCreate(Bundle b) { super.onCreate(b);
  LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(17); root.setBackgroundColor(Color.rgb(8,12,27));
  TextView title = new TextView(this); title.setText("ARTEMIS OS"); title.setTextSize(27); title.setTextColor(Color.WHITE); title.setGravity(17); root.addView(title);
  orb = new Orb(); root.addView(orb, new LinearLayout.LayoutParams(-1, 320));
  status = new TextView(this); status.setText("Inicializando voz..."); status.setTextSize(18); status.setTextColor(Color.WHITE); status.setGravity(17); root.addView(status); setContentView(root);
  tts = new TextToSpeech(this, result -> { if(result == TextToSpeech.SUCCESS) { int lang = tts.setLanguage(new Locale("pt", "BR")); ready = lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED; status.setText(ready ? "Toque na esfera e diga oi" : "Instale uma voz em português nas configurações do Android"); } else status.setText("Voz indisponível neste aparelho"); });
  orb.setOnClickListener(v -> listen());
 }
 private void say(String msg) { status.setText(msg); orb.speaking=true; orb.invalidate(); if(ready) tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "artemis"); orb.postDelayed(() -> {orb.speaking=false;orb.invalidate();}, 2500); }
 private void listen() { if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){status.setText("Reconhecimento de voz não disponível");return;}
  if(recognizer!=null) recognizer.destroy(); recognizer=SpeechRecognizer.createSpeechRecognizer(this);
  recognizer.setRecognitionListener(new RecognitionListener(){
   public void onReadyForSpeech(Bundle p){status.setText("Estou ouvindo...");orb.listening=true;orb.invalidate();}
   public void onBeginningOfSpeech(){} public void onRmsChanged(float rms){} public void onBufferReceived(byte[] b){}
   public void onEndOfSpeech(){orb.listening=false;orb.invalidate();}
   public void onError(int error){orb.listening=false;orb.invalidate();status.setText("Não entendi. Toque para tentar novamente.");}
   public void onResults(Bundle results){orb.listening=false;orb.invalidate();ArrayList<String> words=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);String heard=(words==null||words.isEmpty())?"":words.get(0);say(heard.toLowerCase(Locale.ROOT).contains("oi")||heard.toLowerCase(Locale.ROOT).contains("olá") ? "Olá! Eu sou a Artemis. Que bom te conhecer!" : "Você disse: "+heard);}
   public void onPartialResults(Bundle p){} public void onEvent(int type,Bundle b){}
  });
  Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR");intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  recognizer.startListening(intent);
 }
 @Override protected void onDestroy(){if(recognizer!=null)recognizer.destroy();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
 class Orb extends View { Paint p=new Paint(3); boolean listening=false,speaking=false; Orb(){super(MainActivity.this);} protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.30f; p.setColor(listening?Color.CYAN:speaking?Color.MAGENTA:Color.rgb(83,118,255));p.setShadowLayer(38,0,0,p.getColor());setLayerType(View.LAYER_TYPE_SOFTWARE,p);c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setColor(0x66FFFFFF);c.drawCircle(x-r*.23f,y-r*.25f,r*.33f,p);}}
}
