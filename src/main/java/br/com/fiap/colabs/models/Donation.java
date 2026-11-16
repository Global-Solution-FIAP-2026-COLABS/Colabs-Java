package br.com.fiap.colabs.models;

import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_DONATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private DonationStatus status;

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "RECORD_TYPE", nullable = false, length = 20)
    @Builder.Default
    private RecordType type = RecordType.DONATION_OFFER;

    private String aiSuggestedCategory;
    private String aiTriageReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CITIZEN_ID", nullable = false)
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "COLLECTION_POINT_ID")
    private CollectionPoint collectionPoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CATEGORY_ID", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FULFILLED_BY_CITIZEN_ID")
    private Citizen fulfilledByCitizen;
}