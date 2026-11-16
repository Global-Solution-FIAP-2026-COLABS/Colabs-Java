package br.com.fiap.colabs.config;

import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.models.Category;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.repositories.AccountRepository;
import br.com.fiap.colabs.repositories.CategoryRepository;
import br.com.fiap.colabs.repositories.CitizenRepository;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeederConfig {

    private final CategoryRepository categoryRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final CitizenRepository citizenRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedInitialData() {
        return args -> {
            if (categoryRepository.count() == 0) {
                categoryRepository.save(Category.builder().name("Alimentos").description("Cestas básicas, itens não perecíveis e água potável").build());
                categoryRepository.save(Category.builder().name("Vestuário").description("Roupas, agasalhos, cobertores e calçados em bom estado").build());
                categoryRepository.save(Category.builder().name("Higiene e Limpeza").description("Itens de higiene pessoal, fraldas e produtos de limpeza").build());
                categoryRepository.save(Category.builder().name("Medicamentos e Saúde").description("Itens de primeiros socorros e suprimentos médicos básicos").build());
            }

            CollectionPoint defaultPoint = collectionPointRepository.findAll().stream().findFirst().orElseGet(() ->
                    collectionPointRepository.save(
                            CollectionPoint.builder()
                                    .name("Centro Comunitário Colabs Paulista")
                                    .cnpj("12345678000199")
                                    .address("Av. Paulista, 1106 - Bela Vista")
                                    .neighborhood("Bela Vista")
                                    .phone("(11) 3385-8010")
                                    .active(true)
                                    .build()
                    )
            );

            Citizen defaultCitizen = citizenRepository.findAll().stream().findFirst().orElseGet(() ->
                    citizenRepository.save(
                            Citizen.builder()
                                    .name("Mariana Silva")
                                    .cpf("12345678900")
                                    .phone("(11) 98888-7777")
                                    .address("Rua Augusta, 450 - Consolação")
                                    .build()
                    )
            );

            if (!accountRepository.existsByEmail("admin@colabs.org")) {
                accountRepository.save(
                        Account.builder()
                                .email("admin@colabs.org")
                                .password(passwordEncoder.encode("admin123"))
                                .role(Roles.ROLE_ADMIN)
                                .build()
                );
            }

            if (!accountRepository.existsByEmail("gestor@colabs.org")) {
                accountRepository.save(
                        Account.builder()
                                .email("gestor@colabs.org")
                                .password(passwordEncoder.encode("gestor123"))
                                .role(Roles.ROLE_POINT_MANAGER)
                                .collectionPoint(defaultPoint)
                                .build()
                );
            }

            if (!accountRepository.existsByEmail("cidadao@colabs.org")) {
                accountRepository.save(
                        Account.builder()
                                .email("cidadao@colabs.org")
                                .password(passwordEncoder.encode("cidadao123"))
                                .role(Roles.ROLE_CITIZEN)
                                .citizen(defaultCitizen)
                                .build()
                );
            }

            log.info("[DataSeeder] Base inicial verificada (Admin: admin@colabs.org | Gestor: gestor@colabs.org | Cidadão: cidadao@colabs.org)");
        };
    }
}