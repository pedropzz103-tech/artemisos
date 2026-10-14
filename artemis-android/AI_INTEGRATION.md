# Artemis OS: voz natural e IA local

## Situação
O APK atual prova interface, microfone e TTS. Ainda não executa um modelo de linguagem. Não usar APIs pagas.

## Plano de integração
1. Testar as vozes em português (Brasil) disponíveis no motor TTS do Samsung, nas configurações de síntese de voz. A qualidade e disponibilidade offline dependem do pacote instalado.
2. Integrar um motor de inferência local Android, por exemplo llama.cpp compilado para arm64-v8a, com JNI e execução fora da thread de interface.
3. Testar um modelo GGUF pequeno, como Qwen3-0.6B quantizado em 4 bits, verificando licença, RAM disponível, velocidade e compatibilidade com o motor antes de distribuir. O arquivo do modelo deve ser obtido de fonte oficial e armazenado no dispositivo; não está incluído neste repositório.
4. Fazer o fluxo toque -> ASR -> LLM -> TTS; preferir ASR local quando suportado, mas o SpeechRecognizer padrão pode usar rede.
5. Persistir histórico local e permitir apagar memórias. Comandos de segurança/automação devem ter regras determinísticas, sem confiar cegamente no texto do LLM.

## Critérios de pronto
- Responder a perguntas novas, sem respostas fixas.
- Executar inferência sem internet depois de instalado o modelo.
- Informar claramente quando ASR ou TTS dependerem de rede.
- Não bloquear interface durante geração.

Este documento é planejamento, **não** instalação de IA.
