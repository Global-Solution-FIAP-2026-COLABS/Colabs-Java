package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.enums.DonationCategory;
import br.com.fiap.colabs.enums.RecordType;
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

    public AiTriageResult triageDonation(
            RecordType type,
            String title,
            String description,
            Integer quantity,
            DonationCategory chosenCategory
    ) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-placeholder")) {
            return fallbackTriage(type, title, description, quantity, chosenCategory);
        }

        try {
            String recordLabel = (type == RecordType.HELP_REQUEST)
                    ? "SOLICITAÇÃO DE AJUDA DE CIDADÃO"
                    : "OFERTA DE DOAÇÃO";

            String prompt = """
                    Você é um especialista em triagem humanitária e logística de rede solidária comunitária.
                    Analise o registro abaixo e responda ESTRITAMENTE no formato JSON validado com os campos:
                    - urgency: um de [LOW, MEDIUM, HIGH, CRITICAL]
                    - suggestedCategory: um de [FOOD, CLOTHING, HYGIENE, MEDICINE, FURNITURE, OTHER]
                    - triageReason: justificativa objetiva em até 200 caracteres

                    Tipo de registro: %s
                    Título: %s
                    Descrição: %s
                    Quantidade: %d
                    Categoria informada: %s (%s)
                    """.formatted(recordLabel, title, description, quantity, chosenCategory.name(), chosenCategory.getLabel());

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

        return fallbackTriage(type, title, description, quantity, chosenCategory);
    }

    private AiTriageResult fallbackTriage(
            RecordType type,
            String title,
            String description,
            Integer quantity,
            DonationCategory chosenCategory
    ) {
        String text = (title + " " + description).toLowerCase();
        String prefix = (type == RecordType.HELP_REQUEST) ? "Triagem de pedido: " : "Triagem de doação: ";

        if (chosenCategory == DonationCategory.MEDICINE
                || text.contains("emergência") || text.contains("enchente") || text.contains("remédio")
                || text.contains("medicamento") || text.contains("água") || text.contains("leite infantil")) {
            return new AiTriageResult(
                    UrgencyLevel.CRITICAL,
                    chosenCategory.name(),
                    prefix + "item essencial de resposta imediata ou assistência emergencial."
            );
        }

        if (chosenCategory == DonationCategory.FOOD || chosenCategory == DonationCategory.HYGIENE
                || text.contains("alimento") || text.contains("cesta") || text.contains("cobertor")
                || text.contains("agasalho") || text.contains("higiene") || quantity >= 20) {
            return new AiTriageResult(
                    UrgencyLevel.HIGH,
                    chosenCategory.name(),
                    prefix + "alta demanda comunitária ou lote expressivo de suprimentos básicos."
            );
        }

        if (chosenCategory == DonationCategory.CLOTHING || chosenCategory == DonationCategory.FURNITURE
                || text.contains("roupa") || text.contains("calçado") || text.contains("colchão")
                || type == RecordType.HELP_REQUEST) {
            return new AiTriageResult(
                    UrgencyLevel.MEDIUM,
                    chosenCategory.name(),
                    prefix + "demanda comunitária de uso contínuo com prioridade moderada."
            );
        }

        return new AiTriageResult(
                UrgencyLevel.LOW,
                chosenCategory.name(),
                prefix + "item complementar encaminhado para triagem padrão."
        );
    }
}