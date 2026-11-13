package br.com.fiap.colabs.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

public record CollectionPointRequest(
        @NotBlank(message = "Collection point name is required")
        @Size(min = 3, max = 120, message = "Name must be between 3 and 120 characters")
        String name,

        @NotBlank(message = "CNPJ is required")
        @Pattern(regexp = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}|\\d{14}", message = "CNPJ must be in the format 00.000.000/0000-00 or 14 digits")
        @CNPJ(message = "cnpj is invalid")
        String cnpj,

        @NotBlank(message = "Address is required")
        @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
        String address,

        @NotBlank(message = "Neighborhood is required")
        @Size(min = 2, max = 80, message = "Neighborhood must be between 2 and 80 characters")
        String neighborhood,

        @NotBlank(message = "Phone number is required")
        @Size(min = 8, max = 20, message = "Phone number must be between 8 and 20 characters")
        String phone,

        Boolean active
) {}