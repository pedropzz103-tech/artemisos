package one.visione.artemis;

import android.content.Context;
import java.io.File;

/** Paths for user-provided model assets; do not bundle large models in APK. */
public final class ModelAssets {
 private ModelAssets() {}
 public static File gguf(Context context) { return new File(context.getFilesDir(), "models/artemis-qwen3-0.6b-q4.gguf"); }
 public static File piperOnnx(Context context) { return new File(context.getFilesDir(), "voices/artemis.onnx"); }
 public static File piperConfig(Context context) { return new File(context.getFilesDir(), "voices/artemis.onnx.json"); }
 public static boolean hasAiModel(Context context) { return gguf(context).isFile(); }
 public static boolean hasPiperVoice(Context context) { return piperOnnx(context).isFile() && piperConfig(context).isFile(); }
}
