package br.com.fiap.colabs.models;

import br.com.fiap.colabs.enums.Roles;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_ACCOUNT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Roles role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITIZEN_ID")
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COLLECTION_POINT_ID")
    private CollectionPoint collectionPoint;
}