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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Popula dados de demonstração na primeira execução (somente se ainda não existir nenhuma conta).
 * Para desligar: app.seed.enabled=false
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeederConfig implements CommandLineRunner {

    public static final String ADMIN_EMAIL = "admin@colabs.com";
    public static final String ADMIN_PASSWORD = "Admin@123";
    public static final String MANAGER_EMAIL = "gestor@colabs.com";
    public static final String MANAGER_PASSWORD = "Gestor@123";
    public static final String CITIZEN_EMAIL = "cidadao@colabs.com";
    public static final String CITIZEN_PASSWORD = "Cidadao@123";

    private final AccountRepository accountRepository;
    private final CitizenRepository citizenRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("[Seeder] Já existem contas cadastradas; seed ignorado.");
            return;
        }

        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    category("Alimentos", "Alimentos não perecíveis e cestas básicas"),
                    category("Água e Bebidas", "Água potável e bebidas"),
                    category("Roupas e Agasalhos", "Roupas, cobertores e agasalhos"),
                    category("Higiene", "Itens de higiene pessoal e limpeza"),
                    category("Medicamentos", "Medicamentos e itens de primeiros socorros")
            ));
        }

        CollectionPoint point = collectionPointRepository.save(CollectionPoint.builder()
                .name("Ponto Solidário Centro")
                .cnpj("11.222.333/0001-81")
                .address("Rua das Flores, 100")
                .neighborhood("Centro")
                .phone("1133334444")
                .active(true)
                .build());

        Citizen citizen = citizenRepository.save(Citizen.builder()
                .name("Maria Silva")
                .cpf("529.982.247-25")
                .phone("11987654321")
                .neighborhood("Centro")
                .build());

        accountRepository.save(account(ADMIN_EMAIL, ADMIN_PASSWORD, Roles.ROLE_ADMIN, null, null));
        accountRepository.save(account(MANAGER_EMAIL, MANAGER_PASSWORD, Roles.ROLE_POINT_MANAGER, null, point));
        accountRepository.save(account(CITIZEN_EMAIL, CITIZEN_PASSWORD, Roles.ROLE_CITIZEN, citizen, null));

        log.info("[Seeder] Dados de demonstração criados (admin, gestor e cidadão).");
    }

    private Category category(String name, String description) {
        return Category.builder().name(name).description(description).build();
    }

    private Account account(String email, String rawPassword, Roles role, Citizen citizen, CollectionPoint point) {
        return Account.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .citizen(citizen)
                .collectionPoint(point)
                .build();
    }
}