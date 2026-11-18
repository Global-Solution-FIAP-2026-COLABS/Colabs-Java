package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DonationReviewRequest(
        DonationStatus status,

        @NotBlank(message = "O comentário da avaliação é obrigatório")
        @Size(min = 3, max = 300, message = "A avaliação deve ter entre 3 e 300 caracteres")
        String reviewNotes
) {}