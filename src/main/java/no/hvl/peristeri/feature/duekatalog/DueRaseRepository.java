package no.hvl.peristeri.feature.duekatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DueRaseRepository extends JpaRepository<DueRase, Long> {

    List<DueRase> findAllByOrderByNavnAsc();

    List<DueRase> findByGruppeIdOrderByNavnAsc(Long gruppeId);

    boolean existsByGruppeIdAndNavnIgnoreCase(Long gruppeId, String navn);

    Optional<DueRase> findByGruppeIdAndNavnIgnoreCase(Long gruppeId, String navn);

    long countByGruppeId(Long gruppeId);
}

