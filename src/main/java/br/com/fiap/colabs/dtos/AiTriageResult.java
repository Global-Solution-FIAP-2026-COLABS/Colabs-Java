package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.UrgencyLevel;

public record AiTriageResult(
        UrgencyLevel urgency,
        String suggestedCategory,
        String triageReason
) {}