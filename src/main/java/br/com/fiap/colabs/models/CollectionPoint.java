package br.com.fiap.colabs.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_COLLECTION_POINT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 80)
    private String neighborhood;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}