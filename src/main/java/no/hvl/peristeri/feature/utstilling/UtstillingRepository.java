package no.hvl.peristeri.feature.utstilling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UtstillingRepository extends JpaRepository<Utstilling, Long> {

      @Query("""
          select u from Utstilling u
          where (u.manuellPaameldingStatus is null and u.paameldingsFrist >= CURRENT_DATE)
             or u.manuellPaameldingStatus = true
          order by u.datoRange.startDate
          """)
    List<Utstilling> finnUtstillingerMedAApenPaameldingSortertEtterStartdato();

    @Query("select u from Utstilling u where u.datoRange.startDate >= ?1 order by u.datoRange.startDate")
    List<Utstilling> finnUtstillingerSomStarterEtterGittDato(@NonNull LocalDate startDate);

    @Query("select u from Utstilling u where u.datoRange.endDate <= ?1 order by u.datoRange.startDate DESC")
    List<Utstilling> finnUtstillingerSomEnderFoerGittDato(@NonNull LocalDate endDate);

    @Query("SELECT u FROM Utstilling u ORDER BY u.datoRange.startDate DESC")
    List<Utstilling> finnUtstillingerSortertEtterStartdatoSynkende();

    @Query("SELECT u FROM Utstilling u ORDER BY u.datoRange.startDate ASC")
    List<Utstilling> finnUtstillingerSortertEtterStartdatoStigende();

    @Query("select u from Utstilling u where u.datoRange.endDate >= ?1 order by u.datoRange.startDate")
    List<Utstilling> finnUtstillingerSomAvslutterEtterGittDato(@NonNull LocalDate endDate);

    @Query("select u from Utstilling u where u.aktiv = true")
    Optional<Utstilling> finnAktivUtstilling();

    @Modifying
    @Query("UPDATE Utstilling u SET u.aktiv = false WHERE u.aktiv = true")
    void deaktiverAktiveUtstillinger();
}
