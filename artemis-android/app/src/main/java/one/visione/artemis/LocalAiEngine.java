package one.visione.artemis;

/** Contract for a future on-device model runner. No remote inference is permitted. */
public interface LocalAiEngine {
 boolean isReady();
 String generate(String userMessage) throws Exception;
 void close();
}
