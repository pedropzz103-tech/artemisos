# Artemis OS: integração de IA e voz

## Preparado no código
- Identidade e instruções de segurança em `ArtemisPersona.java`.
- Interface `LocalAiEngine` para conectar um executor de LLM local sem API externa.
- Localização prevista de modelos em `ModelAssets.java`.
- APK inicial mantém reconhecimento de fala e TTS do Android até termos um motor nativo funcional.

## Escolhas técnicas
- IA: [Qwen3-0.6B-GGUF](https://huggingface.co/Qwen/Qwen3-0.6B-GGUF), quantização Q4, licença Apache-2.0. Precisamos integrar biblioteca Android arm64 de inferência, validar formato de prompt e baixar o modelo para o tablet.
- Voz: feminina pt-BR offline, com modelo Piper comunitário compatível e licença individual verificada antes de incorporar. **Ainda não selecionamos um arquivo específico confirmado**. As quatro vozes pt-BR do catálogo Piper original são masculinas, portanto não são adequadas ao pedido. Explorar coleção [OpenVoiceOS](https://huggingface.co/collections/OpenVoiceOS/pipertts-voices-68594d08f08e6ec56eddf4eb).
- A síntese Piper exige ONNX Runtime, fonemização e ponte nativa Android. Apenas colocar arquivos .onnx no tablet não habilita a voz.

## Pendente para ser realmente funcional
1. Escolher e verificar modelo feminino pt-BR e licença, baixar .onnx + .onnx.json.
2. Integrar executor llama.cpp arm64 Android via JNI, carregar GGUF e gerar texto sem rede.
3. Integrar runtime Piper para Android, gerar áudio PCM e reproduzi-lo.
4. Integrar os dois ao MainActivity com tratamento de erros e testes de desempenho.
5. Compilar no GitHub Actions, instalar e testar **em modo avião**.

**Estado:** preparação de integração. A IA e Piper ainda não estão ativos no APK. Não confundir compilação com teste offline concluído.
