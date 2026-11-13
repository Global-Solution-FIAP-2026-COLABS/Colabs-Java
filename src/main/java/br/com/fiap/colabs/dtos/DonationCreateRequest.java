package br.com.fiap.colabs.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DonationCreateRequest(
        @NotBlank(message = "Donation title is required")
        @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
        String title,

        @NotBlank(message = "Donation description is required")
        @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
        String description,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Minimum quantity is 1")
        Integer quantity,

        @NotNull(message = "Citizen ID is required")
        Long citizenId,

        @NotNull(message = "Collection point ID is required")
        Long collectionPointId,

        @NotNull(message = "Category ID is required")
        Long categoryId
) {}