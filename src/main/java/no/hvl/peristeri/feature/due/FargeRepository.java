package no.hvl.peristeri.feature.due;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FargeRepository extends JpaRepository<Farge, Long> {
	Optional<Farge> findByNavnIgnoreCase(String navn);

	List<Farge> findAllByOrderByNavnAsc();
}

