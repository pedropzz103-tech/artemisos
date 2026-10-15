import SwiftUI

struct ChatMessage: Identifiable {
    let id = UUID()
    let role: String
    let text: String
}

struct ContentView: View {
    @State private var message = ""
    @State private var history = [
        ChatMessage(role: "Artemis", text: "Olá! Esta é a interface inicial da Artemis. A IA offline ainda não está conectada.")
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(history) { item in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.role).font(.caption).foregroundStyle(.secondary)
                                Text(item.text)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    .padding()
                }
                HStack {
                    TextField("Escreva para Artemis", text: $message)
                        .textFieldStyle(.roundedBorder)
                        .onSubmit(send)
                    Button("Enviar", action: send)
                        .disabled(message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .padding()
            }
            .navigationTitle("Artemis")
        }
    }

    private func send() {
        let input = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !input.isEmpty else { return }
        history.append(ChatMessage(role: "Você", text: input))
        history.append(ChatMessage(role: "Artemis", text: "Interface em desenvolvimento: ainda sem motor de IA ou controle do alarme."))
        message = ""
    }
}
