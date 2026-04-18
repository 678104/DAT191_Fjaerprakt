package no.hvl.peristeri.feature.duekatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DueGruppeRepository extends JpaRepository<DueGruppe, Long> {

    boolean existsByNavnIgnoreCase(String navn);

    List<DueGruppe> findAllByOrderByNavnAsc();
}

