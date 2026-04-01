package no.hvl.peristeri.feature.due;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RaseRepository extends JpaRepository<Rase, Long> {
	Optional<Rase> findByNavnIgnoreCaseAndGruppeIgnoreCase(String navn, String gruppe);

	List<Rase> findAllByOrderByGruppeAscNavnAsc();
}

