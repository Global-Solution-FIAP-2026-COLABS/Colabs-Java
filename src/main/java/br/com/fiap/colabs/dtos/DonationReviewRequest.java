package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationReviewRequest(
        @NotNull(message = "Novo status da doação é obrigatório")
        DonationStatus status,

        @Size(max = 300, message = "Observação deve ter no máximo 300 caracteres")
        String reviewNotes
) {}