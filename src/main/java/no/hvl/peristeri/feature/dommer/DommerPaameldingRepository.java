package no.hvl.peristeri.feature.dommer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface DommerPaameldingRepository extends JpaRepository<DommerPaamelding, Long> {

	@Query("select d from DommerPaamelding d where d.utstilling.id = ?1")
	List<DommerPaamelding> finnPaameldingerEtterUtstillingId(Long utstillingId);

	@Query("select d from DommerPaamelding d where d.dommer.id = ?1 and d.utstilling.aktiv = true")
	Optional<DommerPaamelding> finnPaameldingForAktivUtstilling(@NonNull Long dommerId);

	Optional<DommerPaamelding> findFirstByDommer_IdAndUtstilling_AktivTrueOrderByUtstilling_DatoRange_StartDateAsc(
			Long dommerId);

	@Query("select d from DommerPaamelding d where d.dommer.id = ?1 order by d.utstilling.datoRange.startDate")
	List<DommerPaamelding> finnPaameldingerEtterDommerId(@NonNull Long dommerId);
}
