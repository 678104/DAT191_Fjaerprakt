package no.hvl.peristeri.feature.duekatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DueVariantRepository extends JpaRepository<DueVariant, Long> {

    boolean existsByNavnIgnoreCase(String navn);

    List<DueVariant> findAllByOrderByNavnAsc();
}

