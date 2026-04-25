package no.hvl.peristeri.feature.bruker;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BedommelseNotifikasjonRepository extends JpaRepository<BedommelseNotifikasjon, Long> {

    boolean existsByMottaker_IdAndDue_IdAndLestTidspunktIsNull(Long mottakerId, Long dueId);

    long countByMottaker_IdAndLestTidspunktIsNull(Long mottakerId);

    @Modifying
    @Transactional
    @Query("""
            update BedommelseNotifikasjon n
               set n.lestTidspunkt = :lestTidspunkt
             where n.mottaker.id = :mottakerId
               and n.due.paamelding.id = :paameldingId
               and n.lestTidspunkt is null
            """)
    int markerSomLestForPaamelding(@Param("mottakerId") Long mottakerId,
                                   @Param("paameldingId") Long paameldingId,
                                   @Param("lestTidspunkt") LocalDateTime lestTidspunkt);
}

