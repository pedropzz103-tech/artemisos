package one.visione.irblaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.ConsumerIrManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int OPEN_IR = 901;
    private static final int MAX_FILE_BYTES = 1600000;
    private static final String SAVED_FILE = "user_ir_library.txt";

    private final int background = Color.rgb(10, 16, 30);
    private final int surface = Color.rgb(25, 35, 54);
    private final int accent = Color.rgb(63, 210, 193);
    private final int foreground = Color.rgb(238, 246, 255);
    private final int muted = Color.rgb(172, 190, 213);

    private final ExecutorService sender = Executors.newSingleThreadExecutor();
    private final ArrayList<IrSignal> presets = new ArrayList<>();
    private final ArrayList<IrSignal> imported = new ArrayList<>();
    private final ArrayList<IrSignal> universal = new ArrayList<>();
    private int universalIndex = 0;
    private TextView universalCounter;
    private ConsumerIrManager transmitter;
    private TextView status;
    private TextView count;
    private LinearLayout libraryButtons;
    private EditText search;
    private boolean hasEmitter;

    static final class IrSignal {
        final String name;
        final String description;
        final int frequency;
        final int[] pulses;
        final int repeats;
        IrSignal(String n, String desc, int f, int[] p, int r) {
            name = n;
            description = desc;
            frequency = f;
            pulses = p;
            repeats = r;
        }
    }

    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        try {
            transmitter = (ConsumerIrManager)getSystemService(Context.CONSUMER_IR_SERVICE);
            hasEmitter = transmitter != null && transmitter.hasIrEmitter();
        } catch (RuntimeException e) {
            hasEmitter = false;
        }
        presets.add(new IrSignal("Samsung TV", "Power 0xE0E040BF · Samsung32",
                38000, pulseDistance(0xE0E040BFL, 32, 4500, 4500, 560, 560, 1690, 560), 1));
        presets.add(new IrSignal("LG TV", "Power 0x20DF10EF · NEC",
                38000, pulseDistance(0x20DF10EFL, 32, 9000, 4500, 560, 560, 1690, 560), 1));
        presets.add(new IrSignal("Sony TV", "Power 0xA90 · SIRC12",
                40000, sony(0xA90, 12), 3));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(30));
        scroll.addView(root);
        setContentView(scroll);

        label(root, "VISIONE  /  IR TOOLS", 12, accent, true, 8);
        label(root, "IR Blaster", 36, foreground, true, 7);
        label(root, "Controle aparelhos pelo infravermelho do Xiaomi.", 15, muted, false, 18);

        LinearLayout state = verticalCard(root);
        label(state, "TRANSMISSOR DO CELULAR", 12, accent, true, 7);
        status = label(state, hasEmitter ? "● Emissor infravermelho detectado" :
                "● Emissor IR indisponível para apps Android", 15,
                hasEmitter ? foreground : Color.rgb(255, 188, 119), true, 9);
        label(state,
                hasEmitter ? "Aponte a parte superior do celular para o aparelho." :
                "Este Xiaomi pode não oferecer acesso público ao IR. Confira o Mi Remote e as permissões do sistema.",
                13, muted, false, 0);

        label(root, "DESLIGAR / POWER", 12, accent, true, 12);
        label(root, "Perfis iniciais de TV. Um comando Power pode ligar um aparelho que já está desligado.",
                13, muted, false, 8);

        for (IrSignal p : presets) {
            button(root, "⏻  " + p.name, surface, foreground,
                    v -> sendOne(p));
            label(root, "      " + p.description, 12, muted, false, 5);
        }
        button(root, "Testar os 3 códigos em sequência", accent, background,
                v -> confirmSweep());

        label(root, "BIBLIOTECA UNIVERSAL DE TV", 12, accent, true, 12);
        label(root, "Mais de 300 comandos Power de aparelhos diferentes. Testa um código por vez. Alguns podem ligar a TV em vez de desligá-la.",
                13, muted, false, 8);
        universalCounter = label(root, "Carregando biblioteca...", 15, foreground, true, 7);
        button(root, "⏻  Testar próximo código universal", accent, background,
                v -> sendNextUniversal());
        button(root, "Voltar ao primeiro código", surface, muted, v -> {
            universalIndex = 0;
            updateUniversalCounter();
        });
        loadUniversal();

        label(root, "COMANDOS .IR", 12, accent, true, 12);
        label(root, "Importa controles do Flipper Zero ou comandos RAW capturados com o ESP32.",
                13, muted, false, 7);
        button(root, "＋  Importar arquivo .ir", surface, foreground,
                v -> openDocument());
        button(root, "Apagar biblioteca importada", surface, muted,
                v -> new AlertDialog.Builder(this)
                        .setTitle("Apagar biblioteca?")
                        .setMessage("Os perfis básicos de TV permanecem disponíveis.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Apagar", (d,w) -> clearLibrary())
                        .show());

        count = label(root, "Nenhum comando importado", 13, muted, true, 9);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Pesquisar botão ou aparelho...");
        search.setTextColor(foreground);
        search.setHintTextColor(muted);
        search.setTextSize(15);
        search.setPadding(dp(13), dp(9), dp(13), dp(9));
        search.setBackground(shape(surface, 12));
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(54)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int c) {
                renderImported();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        libraryButtons = new LinearLayout(this);
        libraryButtons.setOrientation(LinearLayout.VERTICAL);
        root.addView(libraryButtons, new LinearLayout.LayoutParams(-1, -2));
        label(root, "Kosner / Midea: o perfil exato do teu Mi Remote precisa ser capturado e importado. Não existe um código universal garantido.",
                13, muted, false, 0);
        loadSavedLibrary();
        renderImported();
    }

    private void loadUniversal() {
        try (InputStream stream = getAssets().open("universal_tv_power.ir")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = stream.read(b)) != -1) {
                out.write(b, 0, n);
                if (out.size() > MAX_FILE_BYTES) throw new Exception("Biblioteca grande demais");
            }
            universal.addAll(parseFile(new String(out.toByteArray(), StandardCharsets.UTF_8)));
            updateUniversalCounter();
        } catch (Exception e) {
            universalCounter.setText("Biblioteca indisponível: " + e.getMessage());
        }
    }

    private void sendNextUniversal() {
        if (universal.isEmpty()) {
            Toast.makeText(this, "Sem códigos universais disponíveis.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (universalIndex >= universal.size()) {
            new AlertDialog.Builder(this).setTitle("Fim da biblioteca")
                    .setMessage("Todos os códigos disponíveis foram testados. Reiniciar?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Reiniciar", (d,w) -> {
                        universalIndex = 0;
                        updateUniversalCounter();
                    }).show();
            return;
        }
        int sequence = universalIndex + 1;
        IrSignal selected = universal.get(universalIndex++);
        updateUniversalCounter();
        sendOne(new IrSignal("Universal #" + sequence + " (" + selected.description + ")",
                selected.description, selected.frequency, selected.pulses, selected.repeats));
    }

    private void updateUniversalCounter() {
        if (universalCounter == null) return;
        universalCounter.setText("Próximo: " + Math.min(universalIndex + 1, universal.size())
                + " de " + universal.size() + " comandos");
    }

    private void sendOne(IrSignal item) {
        if (!hasEmitter) {
            Toast.makeText(this, "Sem emissor IR acessível no Android.", Toast.LENGTH_LONG).show();
            return;
        }
        status.setText("Transmitindo " + item.name + "...");
        sender.execute(() -> {
            try {
                transmit(item);
                runOnUiThread(() -> status.setText("Sinal enviado: " + item.name));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Falha ao enviar: " + e.getMessage());
                    Toast.makeText(this, "Erro de IR: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void transmit(IrSignal item) throws Exception {
        if (!hasEmitter || transmitter == null) throw new Exception("Emissor IR indisponível");
        long total = 0;
        for (int duration : item.pulses) {
            if (duration <= 0) throw new Exception("Duração IR inválida");
            total += duration;
        }
        if (item.pulses.length < 3 || item.pulses.length > 2048
                || total >= 1950000L) throw new Exception("Sinal grande demais para o Android");
        if (item.frequency < 20000 || item.frequency > 60000)
            throw new Exception("Frequência IR fora de 20-60 kHz");
        for (int i = 0; i < item.repeats; i++) {
            transmitter.transmit(item.frequency, item.pulses);
            if (i + 1 < item.repeats) Thread.sleep(45);
        }
    }

    private void confirmSweep() {
        if (!hasEmitter) {
            Toast.makeText(this, "Sem emissor IR acessível.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Testar os 3 perfis?")
                .setMessage("Vai transmitir Samsung, LG e Sony, com um intervalo entre sinais. Atenção: Power é um botão de alternância e pode LIGAR TVs que já estejam desligadas. Utilize somente nos seus aparelhos ou com autorização.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar teste", (dialog, which) -> sendSweep())
                .show();
    }

    private void sendSweep() {
        sender.execute(() -> {
            try {
                for (int i=0; i<presets.size(); i++) {
                    IrSignal item = presets.get(i);
                    runOnUiThread(() -> status.setText("Testando " + item.name + "..."));
                    transmit(item);
                    if (i+1<presets.size()) Thread.sleep(1200);
                }
                runOnUiThread(() -> status.setText("Sequência concluída. Confira os aparelhos."));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Erro: " + e.getMessage()));
            }
        });
    }

    private void openDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_IR);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != OPEN_IR || result != RESULT_OK || data == null ||
                data.getData() == null) return;
        Uri uri = data.getData();
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new Exception("Arquivo indisponível");
            byte[] buffer = new byte[4096];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int n;
            while ((n=stream.read(buffer)) != -1) {
                out.write(buffer,0,n);
                if (out.size()>MAX_FILE_BYTES) throw new Exception("Arquivo excede 1,6 MB");
            }
            String source = new String(out.toByteArray(), StandardCharsets.UTF_8);
            ArrayList<IrSignal> parsed = parseFile(source);
            if (parsed.isEmpty()) throw new Exception("Sem sinais compatíveis RAW/NEC/Samsung/SIRC");
            try (FileOutputStream file = openFileOutput(SAVED_FILE, MODE_PRIVATE)) {
                file.write(source.getBytes(StandardCharsets.UTF_8));
            }
            imported.clear();
            imported.addAll(parsed);
            search.setText("");
            renderImported();
            status.setText("Biblioteca carregada: " + parsed.size() + " comandos");
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("Importação não concluída")
                    .setMessage(e.getMessage()).setPositiveButton("OK", null).show();
        }
    }

    private void loadSavedLibrary() {
        try (FileInputStream stream = openFileInput(SAVED_FILE)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[4096]; int n;
            while((n=stream.read(b))!=-1) {
                out.write(b,0,n);
                if(out.size() > MAX_FILE_BYTES) return;
            }
            imported.addAll(parseFile(new String(out.toByteArray(),StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            // No saved library on first app launch.
        }
    }

    private void clearLibrary() {
        imported.clear();
        deleteFile(SAVED_FILE);
        renderImported();
        status.setText("Biblioteca importada apagada.");
    }

    private void renderImported() {
        if (libraryButtons == null || count == null || search == null) return;
        libraryButtons.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int showing=0;
        for (IrSignal signal : imported) {
            if(!signal.name.toLowerCase(Locale.ROOT).contains(query) &&
                    !signal.description.toLowerCase(Locale.ROOT).contains(query)) continue;
            if(showing++ >= 70) break;
            button(libraryButtons, "▶  " + signal.name, surface, foreground,
                    v -> sendOne(signal));
        }
        String tail = showing > 70 ? " • exibindo os primeiros 70 (use a busca)" : "";
        count.setText(imported.size() + " comandos importados" + tail);
    }

    private static ArrayList<IrSignal> parseFile(String source) throws Exception {
        if (!source.contains("Filetype: IR signals file") &&
                !source.contains("Filetype: IR library file"))
            throw new Exception("O arquivo não possui cabeçalho .ir Flipper Zero");
        ArrayList<IrSignal> output = new ArrayList<>();
        Map<String,String> fields = new LinkedHashMap<>();
        for (String rawLine : source.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            int pos = line.indexOf(':');
            if (pos <= 0) continue;
            String key = line.substring(0,pos).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(pos+1).trim();
            if (key.equals("name") && fields.containsKey("name")) {
                IrSignal previous = parseEntry(fields);
                if(previous != null) output.add(previous);
                fields.clear();
            }
            fields.put(key,value);
            if(output.size()>=1500) break;
        }
        IrSignal last = parseEntry(fields);
        if(last != null && output.size()<1500) output.add(last);
        return output;
    }

    private static IrSignal parseEntry(Map<String,String> entry) {
        try {
            if(!entry.containsKey("name") || !entry.containsKey("type")) return null;
            String name = entry.get("name").trim();
            if (name.isEmpty()) return null;
            String type = entry.get("type").toLowerCase(Locale.ROOT);
            if(type.equals("raw")) {
                int freq = Integer.parseInt(entry.getOrDefault("frequency","38000"));
                String data = entry.get("data");
                if(data==null || data.length()>12000) return null;
                String[] parts = data.trim().split("\\s+");
                if(parts.length<3 || parts.length>1024) return null;
                int[] pulses = new int[parts.length];
                for(int i=0;i<parts.length;i++) pulses[i]=Integer.parseInt(parts[i]);
                return new IrSignal(name,"Importado · RAW",freq,pulses,1);
            }
            if (!type.equals("parsed")) return null;
            String protocol=entry.getOrDefault("protocol","").trim();
            long address=readHexBytes(entry.getOrDefault("address",""));
            long command=readHexBytes(entry.getOrDefault("command",""));
            int[] pulses;
            int frequency=38000;
            int repeats=1;
            switch(protocol.toUpperCase(Locale.ROOT)) {
                case "NEC": {
                    int a=reverse8((int)address);
                    int c=reverse8((int)command);
                    long bits=((long)a<<24)|((long)(a^0xff)<<16)|((long)c<<8)|(c^0xff);
                    pulses=pulseDistance(bits,32,9000,4500,560,560,1690,560);
                    break;
                }
                case "NECEXT": {
                    int lo=reverse8((int)address);
                    int hi=reverse8((int)(address>>8));
                    int cLo=reverse8((int)command);
                    int cHi=reverse8((int)(command>>8));
                    long bits=((long)lo<<24)|((long)hi<<16)|((long)cLo<<8)|cHi;
                    pulses=pulseDistance(bits,32,9000,4500,560,560,1690,560);
                    break;
                }
                case "SAMSUNG32": {
                    int a=reverse8((int)address);
                    int c=reverse8((int)command);
                    long bits=((long)a<<24)|((long)a<<16)|((long)c<<8)|(c^0xff);
                    pulses=pulseDistance(bits,32,4500,4500,560,560,1690,560);
                    break;
                }
                case "SIRC":
                case "SIRC15":
                case "SIRC20": {
                    int bits=protocol.equalsIgnoreCase("SIRC15")?15:
                            protocol.equalsIgnoreCase("SIRC20")?20:12;
                    long packed=(command&0x7f)|((address&0x1f)<<7);
                    if(bits==15) packed=(command&0x7f)|((address&0xff)<<7);
                    if(bits==20) packed|=((address>>5)&0xff)<<12;
                    pulses=sony(packed,bits);
                    frequency=40000;
                    repeats=3;
                    break;
                }
                default: return null;
            }
            return new IrSignal(name,"Importado · "+protocol,frequency,pulses,repeats);
        } catch (Exception invalid) {
            return null; // An unsupported or invalid button must not break valid ones.
        }
    }

    private static long readHexBytes(String text) {
        String[] bytes=text.trim().split("\\s+");
        if(bytes.length<1 || bytes.length>4) throw new IllegalArgumentException("Hex");
        long value=0;
        for(int i=0;i<bytes.length;i++) {
            int b=Integer.parseInt(bytes[i],16);
            if (b<0 || b>255) throw new IllegalArgumentException("Hex byte");
            value|=((long)b)<<(i*8);
        }
        return value;
    }

    private static int reverse8(int value) {
        return (Integer.reverse(value&0xff)>>>24)&0xff;
    }

    private static int[] pulseDistance(long data,int bits,int headMark,
                                       int headSpace,int bitMark,int zeroSpace,
                                       int oneSpace,int finalMark) {
        int[] p=new int[3+bits*2];
        p[0]=headMark;p[1]=headSpace;
        int j=2;
        for (int bit=bits-1;bit>=0;bit--) {
            p[j++]=bitMark;
            p[j++]=((data>>>bit)&1L)==1L ? oneSpace : zeroSpace;
        }
        p[j]=finalMark;
        return p;
    }

    private static int[] sony(long data,int bits) {
        int[] p=new int[2+bits*2];
        p[0]=2400;p[1]=600;
        int j=2;
        for(int i=0;i<bits;i++) {
            p[j++]=((data>>>i)&1L)==1L ? 1200 : 600;
            p[j++]=600;
        }
        return p;
    }

    private LinearLayout verticalCard(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17),dp(17),dp(17),dp(17));
        card.setBackground(shape(surface,16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2);
        params.bottomMargin = dp(18);
        parent.addView(card,params);
        return card;
    }

    private TextView label(LinearLayout parent,String text,int size,int color,
                           boolean bold,int bottom) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setLineSpacing(dp(2),1f);
        if(bold) view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2);
        params.bottomMargin=dp(bottom);
        parent.addView(view,params);
        return view;
    }

    private void button(LinearLayout parent,String text,int backgroundColor,
                        int textColor,View.OnClickListener onClick) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        button.setBackground(shape(backgroundColor,12));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));
        p.bottomMargin=dp(9);
        parent.addView(button,p);
        button.setOnClickListener(onClick);
    }

    private GradientDrawable shape(int color,int radius) {
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int value) {
        return (int)(value*getResources().getDisplayMetrics().density+0.5f);
    }

    @Override protected void onDestroy() {
        sender.shutdownNow();
        super.onDestroy();
    }
}
