package br.com.fiap.colabs.services;

import br.com.fiap.colabs.dtos.AiTriageResult;
import br.com.fiap.colabs.dtos.DonationCreateRequest;
import br.com.fiap.colabs.dtos.DonationResponse;
import br.com.fiap.colabs.dtos.DonationReviewRequest;
import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.enums.Roles;
import br.com.fiap.colabs.events.DonationEvent;
import br.com.fiap.colabs.events.DonationEventPublisher;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
import br.com.fiap.colabs.models.Citizen;
import br.com.fiap.colabs.models.CollectionPoint;
import br.com.fiap.colabs.models.Donation;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final CitizenRepository citizenRepository;
    private final CollectionPointRepository collectionPointRepository;
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
    public Page<DonationResponse> findCommunityFeed(RecordType type, DonationStatus status, Pageable pageable) {
        if (type != null && status != null) {
            return donationRepository.findByTypeAndStatus(type, status, pageable)
                    .map(DonationResponse::fromEntity);
        }
        if (type != null) {
            return donationRepository.findByType(type, pageable)
                    .map(DonationResponse::fromEntity);
        }
        if (status != null) {
            return donationRepository.findByStatus(status, pageable)
                    .map(DonationResponse::fromEntity);
        }
        return donationRepository.findByStatusIn(
                List.of(DonationStatus.PENDING, DonationStatus.IN_PROGRESS),
                pageable
        ).map(DonationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DonationResponse findById(Long id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado: " + id));
        validateReadAccess(donation);
        return DonationResponse.fromEntity(donation);
    }

    @Transactional
    public DonationResponse create(DonationCreateRequest request) {
        currentUser.requireCitizenAccess(request.citizenId());

        Citizen citizen = citizenRepository.findById(request.citizenId())
                .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + request.citizenId()));

        CollectionPoint point = null;
        if (request.collectionPointId() != null) {
            point = collectionPointRepository.findById(request.collectionPointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + request.collectionPointId()));

            if (!Boolean.TRUE.equals(point.getActive())) {
                throw new BusinessConflictException("O ponto de coleta selecionado está inativo no momento");
            }
        } else if (request.type() == RecordType.DONATION_OFFER) {
            throw new IllegalArgumentException("collectionPointId é obrigatório para ofertas de doação (DONATION_OFFER)");
        }

        AiTriageResult triage = triageAiService.triageDonation(
                request.type(),
                request.title(),
                request.description(),
                request.quantity(),
                request.category()
        );

        Donation donation = Donation.builder()
                .type(request.type())
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
                .category(request.category())
                .build();

        Donation saved = donationRepository.save(donation);
        publishEvent("donation.created", "DONATION_CREATED", saved);

        return DonationResponse.fromEntity(saved);
    }

    @Transactional
    public DonationResponse fulfill(Long id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado: " + id));

        if (donation.getStatus() != DonationStatus.PENDING) {
            throw new BusinessConflictException("Somente registros com status PENDING podem ser assumidos");
        }

        AccountUserDetails user = currentUser.getPrincipal();

        if (user.getRole() == Roles.ROLE_CITIZEN) {
            if (donation.getCitizen().getId().equals(user.getCitizenId())) {
                throw new BusinessConflictException("Você não pode atender sua própria solicitação ou oferta");
            }
            Citizen helper = citizenRepository.findById(user.getCitizenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cidadão não encontrado: " + user.getCitizenId()));
            donation.setFulfilledByCitizen(helper);
        } else if (user.getRole() == Roles.ROLE_POINT_MANAGER) {
            if (donation.getCollectionPoint() != null
                    && !donation.getCollectionPoint().getId().equals(user.getCollectionPointId())) {
                throw new AccessDeniedException("Este registro já está vinculado a outro ponto de coleta");
            }
            CollectionPoint point = collectionPointRepository.findById(user.getCollectionPointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ponto de coleta não encontrado: " + user.getCollectionPointId()));
            if (!Boolean.TRUE.equals(point.getActive())) {
                throw new BusinessConflictException("O ponto de coleta está inativo no momento");
            }
            donation.setCollectionPoint(point);
        } else if (user.getRole() != Roles.ROLE_ADMIN) {
            throw new AccessDeniedException("Perfil sem permissão para atender registros");
        }

        donation.setStatus(DonationStatus.IN_PROGRESS);
        donation.setReviewedAt(LocalDateTime.now());
        donation.setReviewNotes("Atendimento assumido via mural solidário");

        Donation saved = donationRepository.save(donation);
        publishEvent("donation.fulfilled", "DONATION_FULFILLED", saved);

        return DonationResponse.fromEntity(saved);
    }

    @Transactional
    public DonationResponse review(Long id, DonationReviewRequest request) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado: " + id));

        if (donation.getCollectionPoint() != null) {
            currentUser.requireCollectionPointAccess(donation.getCollectionPoint().getId());
        } else if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Apenas administradores ou o ponto vinculado podem revisar este pedido");
        }

        if (donation.getStatus() == DonationStatus.CANCELLED) {
            throw new BusinessConflictException("Não é possível revisar um registro já cancelado");
        }

        if (request.status() == DonationStatus.PENDING
                || request.status() == DonationStatus.IN_PROGRESS
                || request.status() == DonationStatus.CANCELLED) {
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
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado: " + id));

        currentUser.requireCitizenAccess(donation.getCitizen().getId());

        if (donation.getStatus() != DonationStatus.PENDING) {
            throw new BusinessConflictException("Somente registros com status PENDING podem ser cancelados");
        }

        donation.setStatus(DonationStatus.CANCELLED);
        donation.setReviewedAt(LocalDateTime.now());
        donation.setReviewNotes("Cancelado pelo cidadão autor");

        Donation saved = donationRepository.save(donation);
        publishEvent("donation.cancelled", "DONATION_CANCELLED", saved);
    }

    private void validateReadAccess(Donation donation) {
        AccountUserDetails user = currentUser.getPrincipal();
        if (user.getRole() == Roles.ROLE_ADMIN) {
            return;
        }
        if (donation.getStatus() == DonationStatus.PENDING || donation.getStatus() == DonationStatus.IN_PROGRESS) {
            return;
        }
        if (user.getRole() == Roles.ROLE_CITIZEN
                && (donation.getCitizen().getId().equals(user.getCitizenId())
                || (donation.getFulfilledByCitizen() != null
                && donation.getFulfilledByCitizen().getId().equals(user.getCitizenId())))) {
            return;
        }
        if (user.getRole() == Roles.ROLE_POINT_MANAGER
                && donation.getCollectionPoint() != null
                && donation.getCollectionPoint().getId().equals(user.getCollectionPointId())) {
            return;
        }
        throw new AccessDeniedException("Você não tem permissão para visualizar este registro");
    }

    private void publishEvent(String routingKey, String eventType, Donation donation) {
        DonationEvent event = new DonationEvent(
                eventType,
                donation.getId(),
                donation.getTitle(),
                donation.getStatus(),
                donation.getUrgency(),
                donation.getCitizen().getId(),
                donation.getCollectionPoint() != null ? donation.getCollectionPoint().getId() : null,
                LocalDateTime.now()
        );
        eventPublisher.publish(routingKey, event);
    }
}