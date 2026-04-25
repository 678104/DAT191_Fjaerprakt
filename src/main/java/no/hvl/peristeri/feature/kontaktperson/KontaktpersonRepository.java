package no.hvl.peristeri.feature.kontaktperson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KontaktpersonRepository extends JpaRepository<Kontaktperson, Long> {

    List<Kontaktperson> findAllByOrderByRolleAscNavnAsc();

    boolean existsByRolleIgnoreCaseAndNavnIgnoreCase(String rolle, String navn);
}

