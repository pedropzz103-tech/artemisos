# Artemis OS: primeiro protótipo Android

Aplicativo de teste com esfera interativa, reconhecimento de fala pelo Android e resposta por TextToSpeech em português brasileiro. Não há IA local ainda. O reconhecimento de fala **pode exigir internet**, dependendo do serviço instalado no tablet.

## APK
Abra a aba Actions do repositório, escolha **Artemis Android APK**, clique em **Run workflow** e baixe o artefato **artemis-debug-apk** quando o build terminar. Extraia o ZIP e instale o APK no Samsung. O Android poderá pedir permissão para instalar apps dessa origem. Ao abrir, permita acesso ao microfone.

## Observações
A voz disponível depende dos pacotes TTS instalados no tablet. O primeiro teste não possui hotword nem escuta contínua; toque na esfera para falar. Este código está isolado em `artemis-android/` para não alterar o projeto Jarvis existente.
