package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.dtos.DonationCreateRequest;
import br.com.fiap.colabs.dtos.DonationResponse;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.enums.UrgencyLevel;
import br.com.fiap.colabs.events.DonationEventPublisher;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.models.Category;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.models.Donation;
import br.com.fiap.colabs.repositories.CategoryRepository;
import br.com.fiap.colabs.repositories.CitizenRepository;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import br.com.fiap.colabs.repositories.DonationRepository;
import br.com.fiap.colabs.security.AccountUserDetails;
import br.com.fiap.colabs.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;
    @Mock
    private CitizenRepository citizenRepository;
    @Mock
    private CollectionPointRepository collectionPointRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private DonationTriageAiService triageAiService;
    @Mock
    private DonationEventPublisher eventPublisher;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private DonationService donationService;

    @Test
    void shouldCreateDonationOfferWithAiTriageAndPublishRabbitEvent() {
        Citizen citizen = Citizen.builder().id(1L).name("Mariana").cpf("12345678900").phone("11988881111").neighborhood("Centro").build();
        CollectionPoint point = CollectionPoint.builder().id(1L).name("Ponto Centro").cnpj("12345678000199").address("Rua A").neighborhood("Centro").phone("1144332200").active(true).build();
        Category category = Category.builder().id(1L).name("Alimentos").description("Alimentos não perecíveis").build();

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(collectionPointRepository.findById(1L)).thenReturn(Optional.of(point));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(triageAiService.triageDonation(eq(RecordType.DONATION_OFFER), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(new AiTriageResult(UrgencyLevel.HIGH, "Alimentos", "Alta demanda comunitária"));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(42L);
            return d;
        });

        DonationCreateRequest req = new DonationCreateRequest(
                RecordType.DONATION_OFFER, "Cestas Básicas", "Doação de 10 cestas completas", 10, 1L, 1L, 1L
        );
        DonationResponse response = donationService.create(req);

        assertEquals(42L, response.id());
        assertEquals(RecordType.DONATION_OFFER, response.type());
        assertEquals(DonationStatus.PENDING, response.status());
        assertEquals(UrgencyLevel.HIGH, response.urgency());
        verify(eventPublisher).publish(eq("donation.created"), any());
    }

    @Test
    void shouldCreateHelpRequestWithoutCollectionPointAndAllowAnotherCitizenToFulfill() {
        Citizen requester = Citizen.builder().id(1L).name("Mariana").neighborhood("Centro").build();
        Citizen helper = Citizen.builder().id(2L).name("Carlos").neighborhood("Centro").build();
        Category category = Category.builder().id(1L).name("Alimentos").build();

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(triageAiService.triageDonation(eq(RecordType.HELP_REQUEST), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(new AiTriageResult(UrgencyLevel.CRITICAL, "Alimentos", "Pedido urgente"));

        Donation savedRequest = Donation.builder()
                .id(77L)
                .type(RecordType.HELP_REQUEST)
                .title("Leite Infantil")
                .description("Necessidade urgente para bebê")
                .quantity(3)
                .status(DonationStatus.PENDING)
                .urgency(UrgencyLevel.CRITICAL)
                .citizen(requester)
                .category(category)
                .build();

        when(donationRepository.save(any(Donation.class))).thenReturn(savedRequest);

        DonationCreateRequest req = new DonationCreateRequest(
                RecordType.HELP_REQUEST, "Leite Infantil", "Necessidade urgente para bebê", 3, 1L, null, 1L
        );
        DonationResponse created = donationService.create(req);
        assertNull(created.collectionPointId());
        assertEquals(RecordType.HELP_REQUEST, created.type());

        when(donationRepository.findById(77L)).thenReturn(Optional.of(savedRequest));
        Account helperAccount = Account.builder().id(2L).email("carlos@colabs.org").password("x").role(Roles.ROLE_CITIZEN).citizen(helper).build();
        when(currentUser.getPrincipal()).thenReturn(new AccountUserDetails(helperAccount));
        when(citizenRepository.findById(2L)).thenReturn(Optional.of(helper));

        DonationResponse fulfilled = donationService.fulfill(77L);
        assertEquals(DonationStatus.IN_PROGRESS, fulfilled.status());
        assertEquals(2L, fulfilled.fulfilledByCitizenId());
        verify(eventPublisher).publish(eq("donation.fulfilled"), any());
    }

    @Test
    void shouldRejectDonationOfferWhenCollectionPointIsInactive() {
        Citizen citizen = Citizen.builder().id(1L).name("Mariana").build();
        CollectionPoint inactivePoint = CollectionPoint.builder().id(2L).name("Ponto Fechado").active(false).build();

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(collectionPointRepository.findById(2L)).thenReturn(Optional.of(inactivePoint));

        DonationCreateRequest req = new DonationCreateRequest(
                RecordType.DONATION_OFFER, "Roupas", "Casacos de inverno higienizados", 5, 1L, 2L, 1L
        );

        assertThrows(BusinessConflictException.class, () -> donationService.create(req));
        verify(donationRepository, never()).save(any());
    }
}