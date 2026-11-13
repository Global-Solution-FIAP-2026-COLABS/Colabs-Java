package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.UrgencyLevel;
import br.com.fiap.colabs.models.Donation;

import java.time.LocalDateTime;

public record DonationResponse(
        Long id,
        String title,
        String description,
        Integer quantity,
        DonationStatus status,
        UrgencyLevel urgency,
        String aiSuggestedCategory,
        String aiTriageReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String reviewNotes,
        Long citizenId,
        String citizenName,
        Long collectionPointId,
        String collectionPointName,
        Long categoryId,
        String categoryName
) {
    public static DonationResponse fromEntity(Donation d) {
        return new DonationResponse(
                d.getId(),
                d.getTitle(),
                d.getDescription(),
                d.getQuantity(),
                d.getStatus(),
                d.getUrgency(),
                d.getAiSuggestedCategory(),
                d.getAiTriageReason(),
                d.getCreatedAt(),
                d.getReviewedAt(),
                d.getReviewNotes(),
                d.getCitizen().getId(),
                d.getCitizen().getName(),
                d.getCollectionPoint().getId(),
                d.getCollectionPoint().getName(),
                d.getCategory().getId(),
                d.getCategory().getName()
        );
    }
}