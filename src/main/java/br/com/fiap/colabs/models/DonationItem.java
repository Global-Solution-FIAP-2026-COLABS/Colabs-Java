package br.com.fiap.colabs.models;

import br.com.fiap.colabs.enums.DonationCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_DONATION_ITEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false, length = 120)
    private String name;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false, length = 30)
    private DonationCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DONATION_ID", nullable = false)
    private Donation donation;
}