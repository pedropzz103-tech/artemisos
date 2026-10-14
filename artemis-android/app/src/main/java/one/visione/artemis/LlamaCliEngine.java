package one.visione.artemis;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/** Experimental on-device llama.cpp CLI runner. Never sends text to a server. */
public final class LlamaCliEngine implements LocalAiEngine {
 private final Context context;
 public LlamaCliEngine(Context context) { this.context=context.getApplicationContext(); }
 private File executable() { return new File(context.getApplicationInfo().nativeLibraryDir, "libartemis_cli.so"); }
 @Override public boolean isReady() { return ModelAssets.hasAiModel(context) && executable().isFile(); }
 @Override public String generate(String message) throws Exception {
  if(!isReady()) throw new IllegalStateException("Importe o GGUF e instale uma versão com motor nativo.");
  String clean=message.replace("\n"," ").trim();
  if(clean.length()>1000) clean=clean.substring(0,1000);
  String prompt="<|im_start|>system\n"+ArtemisPersona.SYSTEM_PROMPT+"<|im_end|>\n"+
    "<|im_start|>user\n"+clean+"<|im_end|>\n<|im_start|>assistant\n";
  ProcessBuilder builder=new ProcessBuilder(executable().getAbsolutePath(),
    "-m", ModelAssets.gguf(context).getAbsolutePath(), "-p", prompt,
    "-n", "128", "-c", "2048", "-t", "4", "--temp", "0.7", "--no-display-prompt");
  builder.redirectErrorStream(true);
  Process process=builder.start();
  StringBuilder output=new StringBuilder();
  Thread reader=new Thread(() -> {
   try(BufferedReader br=new BufferedReader(new InputStreamReader(process.getInputStream()))) {
    String line; while((line=br.readLine())!=null) { synchronized(output){if(output.length()<32000) output.append(line).append('\n');} }
   }catch(Exception ignored){}
  }); reader.start();
  if(!process.waitFor(90,TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalStateException("A IA demorou demais para responder.");}
  reader.join(1000);
  String raw; synchronized(output){raw=output.toString().trim();}
  if(process.exitValue()!=0) throw new IllegalStateException("Motor local falhou: "+raw.substring(0,Math.min(250,raw.length())));
  // llama-cli output can contain ANSI sequences, status logs and terminal delimiters.
  raw=raw.replaceAll("\\u001B\\[[;\\d]*m","");
  int start=raw.lastIndexOf("<|im_start|>assistant");
  if(start>=0) raw=raw.substring(start+"<|im_start|>assistant".length()).trim();
  int end=raw.indexOf("<|im_end|>"); if(end>=0) raw=raw.substring(0,end);
  return raw.trim().isEmpty()?"Não consegui formular uma resposta.":raw.trim();
 }
 @Override public void close(){}
}
