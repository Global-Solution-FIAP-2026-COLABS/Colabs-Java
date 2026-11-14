package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.dtos.DonationCreateRequest;
import br.com.fiap.colabs.dtos.DonationResponse;
import br.com.fiap.colabs.dtos.DonationReviewRequest;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.events.DonationEvent;
import br.com.fiap.colabs.events.DonationEventPublisher;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final CitizenRepository citizenRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final CategoryRepository categoryRepository;
    private final DonationTriageAiService triageAiService;
    private final DonationEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public Page<DonationResponse> findAll(DonationStatus status, Pageable pageable) {
        AccountUserDetails user = currentUser.getPrincipal();

        if (user.getRole() == Roles.ROLE_CITIZEN) {
            return donationRepository.findByCitizenId(user.getCitizenId(), pageable)
                    .map(DonationResponse::fromEntity);
        }

        if (user.getRole() == Roles.ROLE_POINT_MANAGER) {
            return donationRepository.findByCollectionPointId(user.getCollectionPointId(), pageable)
                    .map(DonationResponse::fromEntity);
        }

        if (status != null) {
            return donationRepository.findByStatus(status, pageable).map(DonationResponse::fromEntity);
        }
        return donationRepository.findAll(pageable).map(DonationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DonationResponse findById(Long id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada: " + id));
        validateReadAccess(donation);
        return DonationResponse.fromEntity(donation);
    }

    @Transactional
    public DonationResponse create(DonationCreateRequest request) {
        currentUser.requireCitizenAccess(request.citizenId());

        Citizen citizen = citizenRepository.findById(request.citizenId())
                .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + request.citizenId()));

        CollectionPoint point = collectionPointRepository.findById(request.collectionPointId())
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + request.collectionPointId()));

        if (!Boolean.TRUE.equals(point.getActive())) {
            throw new BusinessConflictException("O ponto de coleta selecionado está inativo no momento");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + request.categoryId()));

        AiTriageResult triage = triageAiService.triageDonation(
                request.title(),
                request.description(),
                request.quantity(),
                category.getName()
        );

        Donation donation = Donation.builder()
                .title(request.title())
                .description(request.description())
                .quantity(request.quantity())
                .status(DonationStatus.PENDING)
                .urgency(triage.urgency())
                .aiSuggestedCategory(triage.suggestedCategory())
                .aiTriageReason(triage.triageReason())
                .createdAt(LocalDateTime.now())
                .citizen(citizen)
                .collectionPoint(point)
                .category(category)
                .build();

        Donation saved = donationRepository.save(donation);
        publishEvent("donation.created", "DONATION_CREATED", saved);

        return DonationResponse.fromEntity(saved);
    }

    @Transactional
    public DonationResponse review(Long id, DonationReviewRequest request) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada: " + id));

        currentUser.requireCollectionPointAccess(donation.getCollectionPoint().getId());

        if (donation.getStatus() == DonationStatus.CANCELLED) {
            throw new BusinessConflictException("Não é possível revisar uma doação já cancelada");
        }

        if (request.status() == DonationStatus.PENDING || request.status() == DonationStatus.CANCELLED) {
            throw new IllegalArgumentException("Status inválido para revisão. Use APPROVED, REJECTED ou DELIVERED");
        }

        donation.setStatus(request.status());
        donation.setReviewedAt(LocalDateTime.now());
        donation.setReviewNotes(request.reviewNotes());

        Donation saved = donationRepository.save(donation);
        publishEvent("donation.reviewed", "DONATION_REVIEWED", saved);

        return DonationResponse.fromEntity(saved);
    }

    @Transactional
    public void cancel(Long id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada: " + id));

        currentUser.requireCitizenAccess(donation.getCitizen().getId());

        if (donation.getStatus() != DonationStatus.PENDING) {
            throw new BusinessConflictException("Somente doações com status PENDING podem ser canceladas");
        }

        donation.setStatus(DonationStatus.CANCELLED);
        donation.setReviewedAt(LocalDateTime.now());
        donation.setReviewNotes("Cancelada pelo cidadão doador");

        Donation saved = donationRepository.save(donation);
        publishEvent("donation.cancelled", "DONATION_CANCELLED", saved);
    }

    private void validateReadAccess(Donation donation) {
        AccountUserDetails user = currentUser.getPrincipal();
        if (user.getRole() == Roles.ROLE_ADMIN) {
            return;
        }
        if (user.getRole() == Roles.ROLE_CITIZEN
                && donation.getCitizen().getId().equals(user.getCitizenId())) {
            return;
        }
        if (user.getRole() == Roles.ROLE_POINT_MANAGER
                && donation.getCollectionPoint().getId().equals(user.getCollectionPointId())) {
            return;
        }
        throw new AccessDeniedException("Você não tem permissão para visualizar esta doação");
    }

    private void publishEvent(String routingKey, String eventType, Donation donation) {
        DonationEvent event = new DonationEvent(
                eventType,
                donation.getId(),
                donation.getTitle(),
                donation.getStatus(),
                donation.getUrgency(),
                donation.getCitizen().getId(),
                donation.getCollectionPoint().getId(),
                LocalDateTime.now()
        );
        eventPublisher.publish(routingKey, event);
    }
}