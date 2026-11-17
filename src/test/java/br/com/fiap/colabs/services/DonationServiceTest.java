package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.dtos.DonationCreateRequest;
import br.com.fiap.colabs.dtos.DonationItemRequest;
import br.com.fiap.colabs.dtos.DonationResponse;
import br.com.fiap.colabs.enums.DonationCategory;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.enums.UrgencyLevel;
import br.com.fiap.colabs.events.DonationEventPublisher;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.models.Account;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.models.Donation;
import br.com.fiap.colabs.models.DonationItem;
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

import java.util.List;
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
    private DonationTriageAiService triageAiService;
    @Mock
    private DonationEventPublisher eventPublisher;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private DonationService donationService;

    @Test
    void shouldCreateDonationOfferWithMultipleItemsAndPublishRabbitEvent() {
        Citizen citizen = Citizen.builder().id(1L).name("Mariana").cpf("12345678900").phone("11988881111").address("Rua A, 100").build();
        CollectionPoint point = CollectionPoint.builder().id(1L).name("Ponto Centro").cnpj("12345678000199").address("Rua B, 200").phone("1144332200").active(true).build();

        List<DonationItemRequest> items = List.of(
                new DonationItemRequest("Arroz 5kg", 4, DonationCategory.FOOD),
                new DonationItemRequest("Cobertor", 2, DonationCategory.CLOTHING)
        );

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(collectionPointRepository.findById(1L)).thenReturn(Optional.of(point));
        when(triageAiService.triageDonation(eq(RecordType.DONATION_OFFER), anyString(), anyString(), eq(items)))
                .thenReturn(new AiTriageResult(UrgencyLevel.HIGH, "FOOD", "Alta demanda comunitária"));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(42L);
            return d;
        });

        DonationCreateRequest req = new DonationCreateRequest(
                RecordType.DONATION_OFFER, "Cesta e Agasalhos", "Doação de alimentos e cobertores", 1L, 1L, items
        );
        DonationResponse response = donationService.create(req);

        assertEquals(42L, response.id());
        assertEquals(RecordType.DONATION_OFFER, response.type());
        assertEquals(2, response.items().size());
        assertEquals(6, response.totalQuantity());
        assertEquals(DonationStatus.PENDING, response.status());
        assertEquals(UrgencyLevel.HIGH, response.urgency());
        verify(eventPublisher).publish(eq("donation.created"), any());
    }

    @Test
    void shouldCreateHelpRequestWithMultipleItemsAndAllowAnotherCitizenToFulfill() {
        Citizen requester = Citizen.builder().id(1L).name("Mariana").address("Rua A, 100").build();
        Citizen helper = Citizen.builder().id(2L).name("Carlos").address("Rua C, 300").build();

        List<DonationItemRequest> items = List.of(
                new DonationItemRequest("Leite Infantil", 3, DonationCategory.FOOD),
                new DonationItemRequest("Fralda G", 2, DonationCategory.HYGIENE)
        );

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(triageAiService.triageDonation(eq(RecordType.HELP_REQUEST), anyString(), anyString(), eq(items)))
                .thenReturn(new AiTriageResult(UrgencyLevel.CRITICAL, "FOOD", "Pedido urgente"));

        Donation savedRequest = Donation.builder()
                .id(77L)
                .type(RecordType.HELP_REQUEST)
                .title("Apoio para Bebê")
                .description("Necessidade urgente para recém-nascido")
                .status(DonationStatus.PENDING)
                .urgency(UrgencyLevel.CRITICAL)
                .citizen(requester)
                .build();
        savedRequest.addItem(DonationItem.builder().id(1L).name("Leite Infantil").quantity(3).category(DonationCategory.FOOD).build());
        savedRequest.addItem(DonationItem.builder().id(2L).name("Fralda G").quantity(2).category(DonationCategory.HYGIENE).build());

        when(donationRepository.save(any(Donation.class))).thenReturn(savedRequest);

        DonationCreateRequest req = new DonationCreateRequest(
                RecordType.HELP_REQUEST, "Apoio para Bebê", "Necessidade urgente para recém-nascido", 1L, null, items
        );
        DonationResponse created = donationService.create(req);
        assertNull(created.collectionPointId());
        assertEquals(5, created.totalQuantity());

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
        Citizen citizen = Citizen.builder().id(1L).name("Mariana").address("Rua A, 100").build();
        CollectionPoint inactivePoint = CollectionPoint.builder().id(2L).name("Ponto Fechado").active(false).build();

        when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(collectionPointRepository.findById(2L)).thenReturn(Optional.of(inactivePoint));

        DonationCreateRequest req = new DonationCreateRequest(
                RecordType.DONATION_OFFER,
                "Roupas",
                "Casacos de inverno higienizados",
                1L,
                2L,
                List.of(new DonationItemRequest("Casaco", 5, DonationCategory.CLOTHING))
        );

        assertThrows(BusinessConflictException.class, () -> donationService.create(req));
        verify(donationRepository, never()).save(any());
    }
}