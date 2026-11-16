package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.RecordType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationCreateRequest(
        @NotNull(message = "Tipo do registro é obrigatório (DONATION_OFFER ou HELP_REQUEST)")
        RecordType type,

        @NotBlank(message = "Título é obrigatório")
        @Size(min = 3, max = 120, message = "Título deve ter entre 3 e 120 caracteres")
        String title,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 10, max = 500, message = "Descrição deve ter entre 10 e 500 caracteres")
        String description,

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade mínima é 1")
        Integer quantity,

        @NotNull(message = "ID do cidadão é obrigatório")
        Long citizenId,

        Long collectionPointId,

        @NotNull(message = "ID da categoria é obrigatório")
        Long categoryId
) {}