package no.hvl.peristeri.feature.dommer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DommerVinnerRepository extends JpaRepository<DommerVinner, Long> {
	List<DommerVinner> findByDommerPaamelding_IdOrderByTypeAscKategoriNavnAsc(Long dommerPaameldingId);

	List<DommerVinner> findByDommerPaamelding_Utstilling_IdOrderByTypeAscKategoriNavnAsc(Long utstillingId);

	void deleteByDommerPaamelding_Id(Long dommerPaameldingId);
}

