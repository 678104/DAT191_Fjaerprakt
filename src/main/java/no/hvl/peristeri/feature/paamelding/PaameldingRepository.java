package no.hvl.peristeri.feature.paamelding;

import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface PaameldingRepository extends JpaRepository<Paamelding, Long> {

	List<Paamelding> findByUtstiller(@NonNull Bruker utstiller);

	@Query("select p from Paamelding p where p.utstiller = ?1 and p.utstilling = ?2")
	Optional<Paamelding> findByUtstillerAndUtstilling(@NonNull Bruker utstiller, @NonNull Utstilling utstilling);

}
