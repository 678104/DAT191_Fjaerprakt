package no.hvl.peristeri.feature.bruker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface BrukerRepository extends JpaRepository<Bruker, Long> {

    Optional<Bruker> findFirstByFornavnAndEtternavn(@NonNull String fornavn, @NonNull String etternavn);

	List<Bruker> findByFornavnContainingIgnoreCaseOrEtternavnContainingIgnoreCaseOrEpostStartingWithIgnoreCase(
			String fornavn, String etternavn, String epost);

	List<Bruker> findByEpostStartingWithIgnoreCaseOrderByEpostAsc(String epostPrefix);

	List<Bruker> findByFornavnContainingIgnoreCaseOrEtternavnContainingIgnoreCaseOrderByEpostAsc(
			String fornavn, String etternavn);


	Optional<Bruker> findByEpost(String epost);

	List<Bruker> findByRollerContaining(Rolle rolle);

	@Query("select b from Bruker b where :rolle member of b.roller")
	List<Bruker> findAllByRolle(Rolle rolle);

	@Query("select (count(b) > 0) from Bruker b where b.id = :brukerId and :rolle member of b.roller")
	boolean existsByIdAndRolle(Long brukerId, Rolle rolle);
}
