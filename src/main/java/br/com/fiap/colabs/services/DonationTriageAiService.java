package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.enums.UrgencyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DonationTriageAiService {

    private final ChatClient chatClient;
    private final String apiKey;

    public DonationTriageAiService(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.openai.api-key:}") String apiKey
    ) {
        this.chatClient = chatClientBuilder.build();
        this.apiKey = apiKey;
    }

    public AiTriageResult triageDonation(String title, String description, Integer quantity, String chosenCategory) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-placeholder")) {
            return fallbackTriage(title, description, quantity, chosenCategory);
        }

        try {
            String prompt = """
                    Você é um especialista em triagem humanitária e logística de doações comunitárias.
                    Analise a doação abaixo e responda ESTRITAMENTE no formato JSON validado com os campos:
                    - urgency: um de [LOW, MEDIUM, HIGH, CRITICAL]
                    - suggestedCategory: nome curto da categoria mais adequada em português
                    - triageReason: justificativa objetiva em até 200 caracteres

                    Título: %s
                    Descrição: %s
                    Quantidade: %d
                    Categoria informada pelo doador: %s
                    """.formatted(title, description, quantity, chosenCategory);

            AiTriageResult result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(AiTriageResult.class);

            if (result != null && result.urgency() != null) {
                return result;
            }
        } catch (Exception ex) {
            log.warn("[Spring AI] Falha ao consultar OpenAI, acionando triagem determinística de fallback: {}", ex.getMessage());
        }

        return fallbackTriage(title, description, quantity, chosenCategory);
    }

    private AiTriageResult fallbackTriage(String title, String description, Integer quantity, String chosenCategory) {
        String text = (title + " " + description).toLowerCase();

        if (text.contains("emergência") || text.contains("enchente") || text.contains("remédio")
                || text.contains("medicamento") || text.contains("água") || text.contains("leite infantil")) {
            return new AiTriageResult(
                    UrgencyLevel.CRITICAL,
                    chosenCategory,
                    "Triagem automática: item essencial de resposta imediata ou assistência emergencial."
            );
        }

        if (text.contains("alimento") || text.contains("cesta") || text.contains("cobertor")
                || text.contains("agasalho") || text.contains("higiene") || quantity >= 20) {
            return new AiTriageResult(
                    UrgencyLevel.HIGH,
                    chosenCategory,
                    "Triagem automática: alta demanda comunitária ou lote expressivo de suprimentos básicos."
            );
        }

        if (text.contains("roupa") || text.contains("calçado") || text.contains("colchão")) {
            return new AiTriageResult(
                    UrgencyLevel.MEDIUM,
                    chosenCategory,
                    "Triagem automática: item de uso contínuo com prioridade moderada de distribuição."
            );
        }

        return new AiTriageResult(
                UrgencyLevel.LOW,
                chosenCategory,
                "Triagem automática: item complementar encaminhado para triagem padrão do ponto de coleta."
        );
    }
}