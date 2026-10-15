# Artemis OS | Arquitetura da caravana

## Objetivo
Assistente offline no iPhone 12 Pro, painel Android e ESP32 como interface com a central de alarme original. Sem APIs pagas e com operação de segurança independente do telefone.

## Componentes
- **iPhone 12 Pro:** modelo local (candidato Qwen3.5 2B, a validar em desempenho e compatibilidade), reconhecimento de fala local e síntese de voz local. PocketPal AI serve apenas como protótipo, não como integração IoT comprovada.
- **Tablet Android:** painel de estado e controle na rede local.
- **ESP32:** ponte com a central existente; interpreta sinais de estado e executa comandos autorizados, quando a interface elétrica estiver identificada.
- **Central original:** mantém teclado, sensores e sirene operacionais independentemente da Artemis.

## Fluxo pretendido
Voz -> STT local -> LLM local -> validação de comando -> rede Wi-Fi local -> ESP32 -> interface segura da central -> confirmação de estado.

## Segurança elétrica e funcional
- Não ligar sinais de 12 V diretamente aos GPIOs de 3,3 V do ESP32.
- Identificar modelo, bornes, tensões e protocolo da central antes de qualquer ligação.
- Preferir saídas auxiliares documentadas, isolamento e proteção elétrica adequada.
- Manter o acionamento convencional do alarme funcionando sem Wi-Fi, IA ou iPhone.
- Exigir autenticação para desarmar; não executar comandos apenas por texto gerado pelo modelo.

## Próximas etapas
1. Identificar e documentar os bornes da central original.
2. Criar firmware ESP32 com estados simulados, sem conexão elétrica real.
3. Criar protocolo local autenticado entre painel e ESP32.
4. Criar protótipo iOS que envia comandos estruturados, sem depender de acesso direto do PocketPal ao hardware.
5. Integrar STT e TTS locais; medir latência, RAM e consumo.
6. Integrar fisicamente após validação elétrica e testes de falha.

## Estado
Documento inicial de planejamento. Nenhum firmware ou aplicativo foi compilado, instalado ou testado por este commit.
