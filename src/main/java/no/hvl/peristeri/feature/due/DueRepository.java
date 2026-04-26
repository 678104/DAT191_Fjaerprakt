package no.hvl.peristeri.feature.due;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;

public interface DueRepository extends JpaRepository<Due, Long> {

	@Query("""
			SELECT d.rase
			FROM Due d
			LEFT JOIN DueRase dr ON dr.navn = d.rase
			WHERE d.paamelding.utstilling.id = :utstillingId
			GROUP BY d.rase
			ORDER BY CASE WHEN MIN(dr.gruppe.id) IS NULL THEN 1 ELSE 0 END,
			         MIN(dr.gruppe.id),
			         LOWER(d.rase)
			""")
	List<String> hentRaserPaameldtUtstilling(@Param("utstillingId") Long utstillingId);

	List<Due> findByPaamelding_Utstilling_Id(Long utstillingId);

	List<Due> findByPaamelding_Utstilling_IdAndBedommelse_IsNotNull(Long utstillingId);

	List<Due> findByPaamelding_Utstilling_IdOrderByBurnummerAsc(@NonNull Long id);

	List<Due> findByBurnummerOrderByBurnummerAsc(Integer burnummer);

	@Query("SELECT d FROM Due d WHERE d.paamelding.utstilling.id = :utstillingId AND d.burnummer = :burnummer AND d.rase IN :raseFilter")
	Due finnDuePaameldtUtstillingMedBurnummerOgRiktigRase(@NonNull Long utstillingId, @NonNull Integer burnummer,
	                                                      @NonNull Collection<String> raseFilter);

	List<Due> findByPaamelding_Utstilling_IdAndRaseInIgnoreCaseOrderByBurnummerAsc(@NonNull Long id,
	                                                                               @NonNull Collection<String> rases);

	List<Due> findByTildeltDommer_Id(Long dommerPaameldingId);

	@Modifying
	@Query("UPDATE Due d SET d.rase = :nyRase WHERE d.id IN :idListe")
	void updateRaseForIds(@Param("nyRase") @Nullable String nyRase, @Param("idListe") List<Long> idListe);

	@Modifying
	@Query("UPDATE Due d SET d.farge = :nyFarge WHERE d.id IN :idListe")
	void updateFargeForIds(@Param("nyFarge") @Nullable String nyFarge, @Param("idListe") List<Long> idListe);

	@Modifying
	@Query("UPDATE Due d SET d.variant = :nyVariant WHERE d.id IN :idListe")
	void updateVariantForIds(@Param("nyVariant") @Nullable String nyVariant, @Param("idListe") List<Long> idListe);
}
