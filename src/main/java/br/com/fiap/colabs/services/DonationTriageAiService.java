package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.dtos.DonationItemRequest;
import br.com.fiap.colabs.enums.DonationCategory;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.UrgencyLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
            List<DonationItemRequest> items
    ) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-placeholder")) {
            return fallbackTriage(type, title, description, items);
        }

        try {
            String recordLabel = (type == RecordType.HELP_REQUEST)
                    ? "SOLICITAÇÃO DE AJUDA DE CIDADÃO"
                    : "OFERTA DE DOAÇÃO";

            String itemsSummary = items.stream()
                    .map(i -> "- %dx %s [%s]".formatted(i.quantity(), i.name(), i.category().name()))
                    .collect(Collectors.joining("\n"));

            String prompt = """
                    Você é um especialista em triagem humanitária e logística de rede solidária comunitária.
                    Analise o registro com sua lista de itens e responda ESTRITAMENTE no formato JSON validado com os campos:
                    - urgency: um de [LOW, MEDIUM, HIGH, CRITICAL]
                    - suggestedCategory: categoria predominante dentre [FOOD, CLOTHING, HYGIENE, MEDICINE, FURNITURE, OTHER]
                    - triageReason: justificativa objetiva em até 200 caracteres

                    Tipo de registro: %s
                    Título: %s
                    Descrição: %s
                    Itens:
                    %s
                    """.formatted(recordLabel, title, description, itemsSummary);

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

        return fallbackTriage(type, title, description, items);
    }

    private AiTriageResult fallbackTriage(
            RecordType type,
            String title,
            String description,
            List<DonationItemRequest> items
    ) {
        String itemsText = items.stream()
                .map(i -> i.name().toLowerCase())
                .collect(Collectors.joining(" "));
        String text = (title + " " + description + " " + itemsText).toLowerCase();

        DonationCategory primaryCategory = items.isEmpty()
                ? DonationCategory.OTHER
                : items.get(0).category();

        int totalQuantity = items.stream().mapToInt(DonationItemRequest::quantity).sum();
        boolean hasMedicine = items.stream().anyMatch(i -> i.category() == DonationCategory.MEDICINE);
        boolean hasFoodOrHygiene = items.stream().anyMatch(
                i -> i.category() == DonationCategory.FOOD || i.category() == DonationCategory.HYGIENE
        );

        String prefix = (type == RecordType.HELP_REQUEST) ? "Triagem de pedido: " : "Triagem de doação: ";

        if (hasMedicine
                || text.contains("emergência") || text.contains("enchente") || text.contains("remédio")
                || text.contains("medicamento") || text.contains("água") || text.contains("leite infantil")) {
            return new AiTriageResult(
                    UrgencyLevel.CRITICAL,
                    hasMedicine ? DonationCategory.MEDICINE.name() : primaryCategory.name(),
                    prefix + "lote contém item essencial de resposta imediata ou assistência emergencial."
            );
        }

        if (hasFoodOrHygiene
                || text.contains("alimento") || text.contains("cesta") || text.contains("cobertor")
                || text.contains("agasalho") || text.contains("higiene") || totalQuantity >= 20) {
            return new AiTriageResult(
                    UrgencyLevel.HIGH,
                    primaryCategory.name(),
                    prefix + "alta demanda comunitária ou lote expressivo de suprimentos básicos."
            );
        }

        if (type == RecordType.HELP_REQUEST
                || items.stream().anyMatch(i -> i.category() == DonationCategory.CLOTHING || i.category() == DonationCategory.FURNITURE)) {
            return new AiTriageResult(
                    UrgencyLevel.MEDIUM,
                    primaryCategory.name(),
                    prefix + "demanda comunitária de uso contínuo com prioridade moderada."
            );
        }

        return new AiTriageResult(
                UrgencyLevel.LOW,
                primaryCategory.name(),
                prefix + "itens complementares encaminhados para triagem padrão."
        );
    }
}