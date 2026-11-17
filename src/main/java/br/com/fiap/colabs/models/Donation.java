package br.com.fiap.colabs.models;

import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 20)
    private RecordType type;

    @Column(name = "TITLE", nullable = false, length = 120)
    private String title;

    @Column(name = "DESCRIPTION", nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private DonationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "URGENCY", nullable = false, length = 20)
    private UrgencyLevel urgency;

    @Column(name = "AI_SUGGESTED_CATEGORY", length = 60)
    private String aiSuggestedCategory;

    @Column(name = "AI_TRIAGE_REASON", length = 300)
    private String aiTriageReason;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    @Column(name = "REVIEW_NOTES", length = 300)
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CITIZEN_ID", nullable = false)
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COLLECTION_POINT_ID")
    private CollectionPoint collectionPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FULFILLED_BY_CITIZEN_ID")
    private Citizen fulfilledByCitizen;

    @Builder.Default
    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DonationItem> items = new ArrayList<>();

    public void addItem(DonationItem item) {
        items.add(item);
        item.setDonation(this);
    }
}