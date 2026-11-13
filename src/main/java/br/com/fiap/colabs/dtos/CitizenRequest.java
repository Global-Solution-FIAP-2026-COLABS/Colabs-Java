package br.com.fiap.colabs.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

public record CitizenRequest(
        @NotBlank(message = "name is required")
        @Size(min = 3, max = 100, message = "name must have between 3 and 100 characters")
        String name,

        @NotBlank(message = "CPF is required")
        @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{11}", message = "CPF must be in the format 000.000.000-00 or 11 characters")
        @CPF(message = "cpf is invalid")
        String cpf,

        @NotBlank(message = "phone is required")
        @Size(min = 10, max = 12, message = "phone must have between 10 and 12 characters")
        String phone,

        @NotBlank(message = "neighborhood is required")
        @Size(min = 3, message = "neighborhood must have more than 3 characters" )
        String neighborhood
) {}