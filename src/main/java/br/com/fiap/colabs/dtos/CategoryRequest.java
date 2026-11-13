package br.com.fiap.colabs.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(min = 3, max = 80, message = "Name must be between 3 and 80 characters")
        String name,

        @NotBlank(message = "Category description is required")
        @Size(min = 5, max = 255, message = "Description must be between 5 and 255 characters")
        String description
) {}