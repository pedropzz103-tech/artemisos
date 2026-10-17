package one.visione.caravanos;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.Toast;
import java.util.Locale;
public class MainActivity extends Activity {
 Hud hud;
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);hud=new Hud();setContentView(hud);}
 class Hud extends View {
  Canvas c;Paint p=new Paint(3);float sx,sy;String section="Início";
  String[] nav={"Início","Luzes","Água","Clima","Segurança","Energia","Eve","Sistemas"};
  boolean[] lights=new boolean[5];String[] names={"Corredor","Banheiro","Sala","Cozinha","Quarto"};
  int cyan=0xff2ad0ff,white=0xffdbf2ff,muted=0xff7ea7c1,bg=0xff030c1b;
  Hud(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  void color(int v){p.setColor(v);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);p.clearShadowLayer();}
  void rect(float x,float y,float w,float h,int v){color(v);c.drawRoundRect(x,y,x+w,y+h,14,14,p);}
  void frame(float x,float y,float w,float h){rect(x,y,w,h,0xff0a233a);color(cyan);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f);p.setShadowLayer(7,0,0,cyan);c.drawRoundRect(x,y,x+w,y+h,14,14,p);p.clearShadowLayer();p.setStyle(Paint.Style.FILL);}
  void text(String s,float x,float y,int size,int v,boolean bold){color(v);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextSize(size);c.drawText(s,x,y,p);}
  void panel(int x,int y,int w,int h,String s){frame(x,y,w,h);text(s,x+16,y+30,18,white,true);color(0xff126080);c.drawRect(x+16,y+40,x+w-16,y+41,p);}
  @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);c=canvas;sx=getWidth()/1280f;sy=getHeight()/800f;c.save();c.scale(sx,sy);c.drawColor(bg);
   color(0xff0b2233);for(int i=0;i<1280;i+=40)c.drawLine(i,0,i,800,p);for(int i=0;i<800;i+=40)c.drawLine(0,i,1280,i,p);
   frame(12,12,1256,776);text("CARAVANOS",493,70,37,white,true);text("LIBERDADE  •  CONFORTO  •  SEGURANÇA",452,99,14,cyan,false);
   text("CARAVANOS / OFFLINE",40,60,18,cyan,true);text("Protótipo local, sem ligação aos sensores",40,87,13,muted,false);
   frame(28,123,230,641);for(int i=0;i<nav.length;i++){int y=141+i*75;if(section.equals(nav[i]))rect(42,y,201,58,0xff105475);text(nav[i],64,y+37,21,section.equals(nav[i])?cyan:white,section.equals(nav[i]));}
   if(section.equals("Início"))home();else detail();text("CaravanOS v0.1  •  Modo demonstração",915,776,12,muted,false);c.restore();
  }
  void home(){panel(280,123,595,390,"VISÃO GERAL");van(580,328);panel(897,123,344,390,"ESTADO DOS SISTEMAS");
   String[] l={"Alarme","Luzes","Água","Climatização","Energia"},v={"Não conectado",count()+" de 5 (simulado)","Sem sensor","Sem sensor","Rede externa: não verificada"};
   for(int i=0;i<l.length;i++){int y=212+i*58;text(l[i],918,y,18,white,true);text(v[i],918,y+24,15,muted,false);}
   panel(280,532,295,222,"SEGURANÇA");text("Estado desconhecido",300,602,20,0xffffb85c,true);text("Central não integrada",300,636,16,muted,false);
   panel(592,532,294,222,"CLIMA");text("Temperatura: --",613,602,20,white,true);text("A/C: sem integração",613,636,16,muted,false);
   panel(902,532,339,222,"EVE");text("Assistente offline",923,602,20,white,true);text("Voz ainda não integrada",923,636,16,muted,false);
  }
  int count(){int n=0;for(boolean v:lights)if(v)n++;return n;}
  void van(int x,int y){color(0xff113d59);c.drawCircle(x,y,175,p);color(cyan);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);Path path=new Path();path.moveTo(x-190,y+60);path.lineTo(x-190,y-80);path.lineTo(x+40,y-80);path.lineTo(x+95,y-35);path.lineTo(x+172,y-20);path.lineTo(x+185,y+60);path.close();c.drawPath(path,p);c.drawRect(x-165,y-57,x-25,y+14,p);c.drawRect(x+40,y-18,x+115,y+29,p);c.drawCircle(x-110,y+65,29,p);c.drawCircle(x+120,y+65,29,p);p.setStyle(Paint.Style.FILL);text("MODELO ILUSTRATIVO",x-90,y+128,13,muted,false);}
  void detail(){panel(280,123,961,631,section.toUpperCase(Locale.ROOT));
   if(section.equals("Luzes")){text("Toque para alternar a simulação. Nenhum circuito físico será acionado.",307,207,18,muted,false);for(int i=0;i<5;i++){int x=306+(i%3)*303,y=244+(i/3)*185;frame(x,y,274,157);text(names[i],x+21,y+44,23,white,true);text(lights[i]?"SIMULADO: LIGADO":"SIMULADO: DESLIGADO",x+21,y+99,16,lights[i]?0xff37f6bd:muted,true);}}
   else if(section.equals("Segurança")){text("Alarme: ESTADO DESCONHECIDO",312,218,28,0xffffb85c,true);text("A central Ademco ainda não está integrada.",312,273,19,muted,false);text("Não considere o sistema armado com base nesta tela.",312,321,19,white,false);}
   else if(section.equals("Eve")){text("Eve  •  módulo de voz offline",312,218,28,cyan,true);text("Interface reservada para o modelo local.",312,277,19,muted,false);text("A ativação por palavra-chave ainda não está implementada.",312,324,18,white,false);}
   else {text("Painel preparado para futura integração com hardware.",312,225,22,white,false);text("Ainda não existem leituras em tempo real nesta versão.",312,274,19,muted,false);}
  }
  @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()/sx,y=e.getY()/sy;
   if(x>=28&&x<258&&y>=141&&y<741){int i=(int)((y-141)/75);if(i>=0&&i<nav.length){section=nav[i];invalidate();}return true;}
   if(section.equals("Luzes"))for(int i=0;i<5;i++){int bx=306+(i%3)*303,by=244+(i/3)*185;if(x>=bx&&x<bx+274&&y>=by&&y<by+157){lights[i]=!lights[i];invalidate();Toast.makeText(MainActivity.this,"Simulação: "+names[i],Toast.LENGTH_SHORT).show();break;}}
   return true;
  }
 }
}