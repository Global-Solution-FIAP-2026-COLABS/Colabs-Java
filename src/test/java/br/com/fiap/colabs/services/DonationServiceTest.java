package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.dtos.DonationCreateRequest;
import br.com.fiap.colabs.dtos.DonationResponse;
import br.com.fiap.colabs.dtos.DonationReviewRequest;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.UrgencyLevel;
import br.com.fiap.colabs.events.DonationEvent;
import br.com.fiap.colabs.events.DonationEventPublisher;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.models.Category;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.models.Donation;
import br.com.fiap.colabs.repositories.CategoryRepository;
import br.com.fiap.colabs.repositories.CitizenRepository;
import br.com.fiap.colabs.repositories.CollectionPointRepository;
import br.com.fiap.colabs.repositories.DonationRepository;
import br.com.fiap.colabs.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock private DonationRepository donationRepository;
    @Mock private CitizenRepository citizenRepository;
    @Mock private CollectionPointRepository collectionPointRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private DonationTriageAiService triageAiService;
    @Mock private DonationEventPublisher eventPublisher;
    @Mock private CurrentUser currentUser;

    @InjectMocks private DonationService donationService;

    private final Citizen citizen = Citizen.builder().id(1L).name("Maria Silva").build();
    private final Category category = Category.builder().id(3L).name("Alimentos").build();

    private CollectionPoint point(boolean active) {
        return CollectionPoint.builder().id(2L).name("Ponto Centro").active(active).build();
    }

    private Donation donation(DonationStatus status) {
        return Donation.builder()
                .id(9L).title("Cestas").description("20 cestas básicas").quantity(20)
                .status(status).urgency(UrgencyLevel.HIGH)
                .citizen(citizen).collectionPoint(point(true)).category(category)
                .build();
    }

    private DonationCreateRequest createRequest() {
        return new DonationCreateRequest("Cestas básicas", "20 cestas básicas para famílias", 20, 1L, 2L, 3L);
    }

    @Test
    void create_appliesAiTriageAndPublishesCreatedEvent() {
        when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(collectionPointRepository.findById(2L)).thenReturn(Optional.of(point(true)));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(triageAiService.triageDonation(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(new AiTriageResult(UrgencyLevel.CRITICAL, "Alimentos", "Item essencial"));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(9L);
            return d;
        });

        DonationResponse response = donationService.create(createRequest());

        assertEquals(DonationStatus.PENDING, response.status());
        assertEquals(UrgencyLevel.CRITICAL, response.urgency());
        assertEquals("Item essencial", response.aiTriageReason());
        verify(currentUser).requireCitizenAccess(1L);
        verify(eventPublisher).publish(eq("donation.created"), any(DonationEvent.class));
    }

    @Test
    void create_withInactivePoint_throwsConflictBeforeTriage() {
        when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
        when(collectionPointRepository.findById(2L)).thenReturn(Optional.of(point(false)));

        assertThrows(BusinessConflictException.class, () -> donationService.create(createRequest()));

        verify(triageAiService, never()).triageDonation(anyString(), anyString(), anyInt(), anyString());
        verify(donationRepository, never()).save(any());
    }

    @Test
    void review_approvesDonationAndPublishesReviewedEvent() {
        when(donationRepository.findById(9L)).thenReturn(Optional.of(donation(DonationStatus.PENDING)));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        DonationResponse response = donationService.review(9L, new DonationReviewRequest(DonationStatus.APPROVED, "ok"));

        assertEquals(DonationStatus.APPROVED, response.status());
        verify(currentUser).requireCollectionPointAccess(2L);
        verify(eventPublisher).publish(eq("donation.reviewed"), any(DonationEvent.class));
    }

    @Test
    void review_cancelledDonation_throwsConflict() {
        when(donationRepository.findById(9L)).thenReturn(Optional.of(donation(DonationStatus.CANCELLED)));

        assertThrows(BusinessConflictException.class,
                () -> donationService.review(9L, new DonationReviewRequest(DonationStatus.APPROVED, null)));
    }

    @Test
    void review_withPendingStatus_isRejected() {
        when(donationRepository.findById(9L)).thenReturn(Optional.of(donation(DonationStatus.PENDING)));

        assertThrows(IllegalArgumentException.class,
                () -> donationService.review(9L, new DonationReviewRequest(DonationStatus.PENDING, null)));

        verify(donationRepository, never()).save(any());
    }

    @Test
    void cancel_pendingDonation_setsCancelledAndPublishesEvent() {
        Donation pending = donation(DonationStatus.PENDING);
        when(donationRepository.findById(9L)).thenReturn(Optional.of(pending));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        donationService.cancel(9L);

        assertEquals(DonationStatus.CANCELLED, pending.getStatus());
        verify(currentUser).requireCitizenAccess(1L);
        verify(eventPublisher).publish(eq("donation.cancelled"), any(DonationEvent.class));
    }

    @Test
    void cancel_nonPendingDonation_throwsConflict() {
        when(donationRepository.findById(9L)).thenReturn(Optional.of(donation(DonationStatus.APPROVED)));

        assertThrows(BusinessConflictException.class, () -> donationService.cancel(9L));

        verify(donationRepository, never()).save(any());
    }
}