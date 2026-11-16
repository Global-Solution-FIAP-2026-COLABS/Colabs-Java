package br.com.fiap.colabs.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_CITIZEN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "ADDRESS", length = 200)
    private String address;
}