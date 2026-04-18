package no.hvl.peristeri.feature.dommer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DommerVinnerRepository extends JpaRepository<DommerVinner, Long> {
	List<DommerVinner> findByDommerPaamelding_IdOrderByTypeAscKategoriNavnAsc(Long dommerPaameldingId);

	void deleteByDommerPaamelding_Id(Long dommerPaameldingId);
}

