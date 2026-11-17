package br.com.fiap.colabs.dtos;

import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.UrgencyLevel;
import br.com.fiap.colabs.models.Donation;
import br.com.fiap.colabs.models.DonationItem;

import java.time.LocalDateTime;
import java.util.List;

public record DonationResponse(
        Long id,
        RecordType type,
        String title,
        String description,
        Integer totalQuantity,
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
        List<DonationItemResponse> items
) {
    public static DonationResponse fromEntity(Donation d) {
        List<DonationItem> entityItems = d.getItems() != null ? d.getItems() : List.of();
        int totalQty = entityItems.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();

        return new DonationResponse(
                d.getId(),
                d.getType(),
                d.getTitle(),
                d.getDescription(),
                totalQty,
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
                entityItems.stream().map(DonationItemResponse::fromEntity).toList()
        );
    }
}