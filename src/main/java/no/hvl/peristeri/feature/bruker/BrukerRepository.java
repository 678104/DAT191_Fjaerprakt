package no.hvl.peristeri.feature.bruker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface BrukerRepository extends JpaRepository<Bruker, Long> {

    Optional<Bruker> findFirstByFornavnAndEtternavn(@NonNull String fornavn, @NonNull String etternavn);


	Optional<Bruker> findByEpost(String epost);
}
