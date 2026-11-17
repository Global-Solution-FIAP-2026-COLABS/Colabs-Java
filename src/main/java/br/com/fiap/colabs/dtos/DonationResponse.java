package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationCategory;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.UrgencyLevel;
import br.com.fiap.colabs.models.Donation;

import java.time.LocalDateTime;

public record DonationResponse(
        Long id,
        RecordType type,
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
        String citizenAddress,
        Long collectionPointId,
        String collectionPointName,
        Long fulfilledByCitizenId,
        String fulfilledByCitizenName,
        DonationCategory category,
        String categoryLabel
) {
    public static DonationResponse fromEntity(Donation d) {
        return new DonationResponse(
                d.getId(),
                d.getType(),
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
                d.getCitizen().getAddress(),
                d.getCollectionPoint() != null ? d.getCollectionPoint().getId() : null,
                d.getCollectionPoint() != null ? d.getCollectionPoint().getName() : null,
                d.getFulfilledByCitizen() != null ? d.getFulfilledByCitizen().getId() : null,
                d.getFulfilledByCitizen() != null ? d.getFulfilledByCitizen().getName() : null,
                d.getCategory(),
                d.getCategory().getLabel()
        );
    }
}