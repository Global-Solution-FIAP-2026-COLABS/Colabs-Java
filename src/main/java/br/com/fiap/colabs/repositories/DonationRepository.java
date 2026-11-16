package br.com.fiap.colabs.repositories;

import br.com.fiap.colabs.enums.DonationStatus;
import br.com.fiap.colabs.enums.RecordType;
import br.com.fiap.colabs.models.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    Page<Donation> findByCitizenId(Long citizenId, Pageable pageable);
    Page<Donation> findByCollectionPointId(Long collectionPointId, Pageable pageable);
    Page<Donation> findByStatus(DonationStatus status, Pageable pageable);
    Page<Donation> findByType(RecordType type, Pageable pageable);
    Page<Donation> findByTypeAndStatus(RecordType type, DonationStatus status, Pageable pageable);
    Page<Donation> findByStatusIn(List<DonationStatus> statuses, Pageable pageable);
}