package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationItemRequest(
        @NotBlank(message = "Nome do item é obrigatório")
        @Size(min = 2, max = 120, message = "Nome do item deve ter entre 2 e 120 caracteres")
        String name,

        @NotNull(message = "Quantidade do item é obrigatória")
        @Min(value = 1, message = "Quantidade mínima de cada item é 1")
        Integer quantity,

        @NotNull(message = "Categoria do item é obrigatória")
        DonationCategory category
) {}