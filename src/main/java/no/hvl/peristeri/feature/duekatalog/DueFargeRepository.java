package no.hvl.peristeri.feature.duekatalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DueFargeRepository extends JpaRepository<DueFarge, Long> {

    boolean existsByNavnIgnoreCase(String navn);

    List<DueFarge> findAllByOrderByNavnAsc();
}

