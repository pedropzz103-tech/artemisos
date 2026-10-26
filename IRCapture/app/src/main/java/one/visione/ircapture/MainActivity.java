package one.visione.ircapture;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int EXPORT_CODE = 112;
    private static final String HOST = "http://192.168.4.1";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ArrayList<Signal> signals = new ArrayList<>();
    private TextView status, summary, listing;
    private EditText name;
    private boolean active = false;
    private boolean waiting = false;
    private boolean polling = false;
    private int baselineId = -1;
    private final int bg = Color.rgb(15, 21, 36);
    private final int panel = Color.rgb(29, 37, 56);
    private final int light = Color.rgb(238, 243, 252);
    private final int muted = Color.rgb(177, 191, 212);
    private final int accent = Color.rgb(77, 205, 185);

    static class Signal {
        final String name;
        final int frequency;
        final int[] timing;
        Signal(String n, int f, int[] t) { name = n; frequency = f; timing = t; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(32));
        scroll.addView(root);
        setContentView(scroll);

        text(root, "VISIONE  /  LAB", 12, accent, true, 8);
        text(root, "IR Capture", 34, light, true, 5);
        text(root, "Aprenda os sinais do Mi Remote e exporte um arquivo .ir", 15, muted, false, 22);

        LinearLayout card = new LinearLayout(this);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(shape(panel, 16));
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
        text(card, "RECEPTOR ESP32", 12, accent, true, 8);
        status = text(card, "Conectando a 192.168.4.1...", 15, light, true, 12);
        text(card, "1. Instale o firmware e ligue um receptor IR no GPIO 27.\n2. Conecte o Xiaomi ao Wi-Fi IR-CAPTURE.\n3. Arme a captura e pressione um botão no Mi Remote.", 14, muted, false, 4);

        text(root, "NOME DO COMANDO", 12, accent, true, 10);
        name = new EditText(this);
        name.setSingleLine(true);
        name.setText("Kosner_Frio_24");
        name.setTextSize(16);
        name.setTextColor(light);
        name.setHintTextColor(muted);
        name.setPadding(dp(12), dp(10), dp(12), dp(10));
        name.setBackground(shape(panel, 10));
        root.addView(name, new LinearLayout.LayoutParams(-1, dp(53)));

        button(root, "●  CAPTURAR PRÓXIMO SINAL", accent, bg, v -> arm());
        button(root, "Exportar arquivo .ir", panel, light, v -> export());
        button(root, "Limpar comandos", panel, muted, v -> {
            signals.clear(); waiting = false; refresh();
        });

        text(root, "CAPTURAS", 12, accent, true, 12);
        summary = text(root, "0 comandos", 19, light, true, 10);
        listing = text(root, "Nenhum sinal capturado ainda.", 14, muted, false, 0);
        refresh();
        active = true;
        handler.post(poller);
    }

    private final Runnable poller = new Runnable() {
        @Override public void run() {
            if (!active) return;
            if (!polling) {
                polling = true;
                io.execute(() -> {
                    try {
                        JSONObject obj = get("/api/latest");
                        runOnUiThread(() -> onPoll(obj));
                    } catch (Exception e) {
                        runOnUiThread(() -> status.setText("Sem conexão. Conecte ao Wi-Fi IR-CAPTURE."));
                    } finally { polling = false; }
                });
            }
            handler.postDelayed(this, 1100);
        }
    };

    private void onPoll(JSONObject obj) {
        int id = obj.optInt("id", 0);
        if (baselineId < 0) baselineId = id;
        if (!waiting) {
            status.setText("Conectado • receptor pronto");
            return;
        }
        status.setText("Aguardando sinal infravermelho...");
        if (id <= baselineId) return;
        baselineId = id;
        waiting = false;
        if (obj.optBoolean("overflow", false)) {
            status.setText("Sinal grande demais. Repita a captura.");
            Toast.makeText(this, "Sinal ultrapassou o limite. Tente de novo.", Toast.LENGTH_LONG).show();
            return;
        }
        JSONArray timings = obj.optJSONArray("data");
        if (timings == null || timings.length() < 10 || timings.length() > 1024) {
            status.setText("Sinal inválido. Tente novamente.");
            return;
        }
        int[] raw = new int[timings.length()];
        for (int i=0; i<raw.length; i++) {
            raw[i] = timings.optInt(i, 0);
            if (raw[i] <= 0) { status.setText("Sinal com duração inválida."); return; }
        }
        String label = safeName(name.getText().toString().trim());
        signals.add(new Signal(label, obj.optInt("frequency", 38000), raw));
        status.setText("Capturado: " + label + " (" + raw.length + " pulsos)");
        refresh();
        Toast.makeText(this, "Comando salvo na lista!", Toast.LENGTH_SHORT).show();
    }

    private void arm() {
        String label = safeName(name.getText().toString().trim());
        if (label.isEmpty()) { Toast.makeText(this,"Digite o nome do comando",Toast.LENGTH_SHORT).show(); return; }
        status.setText("Preparando captura...");
        io.execute(() -> {
            try {
                JSONObject obj = get("/api/arm");
                runOnUiThread(() -> {
                    baselineId = obj.optInt("id", 0);
                    waiting = true;
                    status.setText("Pronto! Aponte o Xiaomi para o receptor e aperte o botão.");
                });
            } catch(Exception e) {
                runOnUiThread(() -> {
                    waiting = false;
                    status.setText("Não encontrei o ESP32. Verifique o Wi-Fi.");
                });
            }
        });
    }

    private JSONObject get(String path) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)new URL(HOST + path).openConnection();
        conn.setConnectTimeout(2000); conn.setReadTimeout(2000);
        try {
            if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());
            byte[] bytes = readBytes(conn.getInputStream());
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } finally { conn.disconnect(); }
    }

    private static byte[] readBytes(java.io.InputStream stream) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096]; int n;
        try (java.io.InputStream in = stream) {
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if(out.size()>100000) throw new Exception("Resposta excede limite");
            }
        }
        return out.toByteArray();
    }

    private void export() {
        if (signals.isEmpty()) {
            Toast.makeText(this,"Capture pelo menos um sinal.",Toast.LENGTH_SHORT).show(); return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "Kosner_Midea.ir");
        startActivityForResult(intent, EXPORT_CODE);
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == EXPORT_CODE && result == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("Sem permissão de escrita");
                out.write(makeIr().getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this,"Arquivo .ir exportado!",Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this,"Erro ao exportar: " + e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
    }

    private String makeIr() {
        StringBuilder sb = new StringBuilder("Filetype: IR signals file\nVersion: 1\n");
        for (Signal s : signals) {
            sb.append("#\nname: ").append(s.name).append("\n");
            sb.append("type: raw\nfrequency: ").append(s.frequency).append("\n");
            sb.append("duty_cycle: 0.330000\ndata: ");
            for (int i = 0; i < s.timing.length; i++) {
                if(i>0) sb.append(' ');
                sb.append(s.timing[i]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private void refresh() {
        summary.setText(signals.size() + (signals.size()==1 ? " comando" : " comandos"));
        if(signals.isEmpty()) { listing.setText("Nenhum sinal capturado ainda."); return; }
        StringBuilder b=new StringBuilder();
        for(int i=0;i<signals.size();i++) {
            Signal s=signals.get(i);
            b.append(String.format(Locale.ROOT, "%02d   %s  ·  %d pulsos\n",i+1,s.name,s.timing.length));
        }
        listing.setText(b.toString().trim());
    }

    private static String safeName(String name) {
        String value = name.replaceAll("[^A-Za-z0-9_-]", "_");
        while(value.contains("__")) value=value.replace("__","_");
        if(value.length()>35) value=value.substring(0,35);
        return value;
    }

    private TextView text(LinearLayout parent,String txt,int size,int color,boolean bold,int bottom) {
        TextView t = new TextView(this);
        t.setText(txt); t.setTextSize(size); t.setTextColor(color);
        t.setLineSpacing(dp(3),1.0f);
        if(bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(bottom); parent.addView(t,p); return t;
    }

    private void button(LinearLayout parent,String txt,int color,int ink, View.OnClickListener action) {
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(txt); b.setTextColor(ink); b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(color,12));
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(55));
        p.topMargin=dp(13); parent.addView(b,p); b.setOnClickListener(action);
    }

    private GradientDrawable shape(int color,int radius) {
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(radius));return g;
    }
    private int dp(float value) { return (int)(value*getResources().getDisplayMetrics().density+0.5f); }
    @Override protected void onDestroy() {
        active=false;
        handler.removeCallbacks(poller);
        io.shutdownNow();
        super.onDestroy();
    }
}
