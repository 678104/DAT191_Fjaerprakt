package no.hvl.peristeri.feature.dommer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BedommelseBildeRepository extends JpaRepository<BedommelseBilde, Long> {
	Optional<BedommelseBilde> findByBedommelse_Due_Id(Long dueId);

	boolean existsByBedommelse_Due_Id(Long dueId);
}

