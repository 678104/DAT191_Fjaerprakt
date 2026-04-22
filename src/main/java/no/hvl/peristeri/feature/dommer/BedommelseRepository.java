package no.hvl.peristeri.feature.dommer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BedommelseRepository extends JpaRepository<Bedommelse, Long> {
	List<Bedommelse> findByBedomtAv_Id(Long dommerPaameldingId);
}
