package br.com.fiap.colabs.events;

import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.UrgencyLevel;

import java.time.LocalDateTime;

public record DonationEvent(
        String eventType,
        Long donationId,
        String title,
        DonationStatus status,
        UrgencyLevel urgency,
        Long citizenId,
        Long collectionPointId,
        LocalDateTime occurredAt
) {}