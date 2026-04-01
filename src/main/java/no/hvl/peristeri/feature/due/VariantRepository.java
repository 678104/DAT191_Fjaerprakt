package no.hvl.peristeri.feature.due;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, Long> {
	Optional<Variant> findByNavnIgnoreCase(String navn);

	List<Variant> findAllByOrderByNavnAsc();
}

