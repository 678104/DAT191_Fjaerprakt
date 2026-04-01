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

	@Query("SELECT DISTINCT CONCAT(d.raseLookup.navn, ' (', d.raseLookup.gruppe, ')') FROM Due d JOIN d.paamelding p WHERE p.utstilling.id = :utstillingId AND d.raseLookup IS NOT NULL")
	List<String> hentRaserPaameldtUtstilling(@Param("utstillingId") Long utstillingId);

	List<Due> findByPaamelding_Utstilling_Id(Long utstillingId);

	List<Due> findByPaamelding_Utstilling_IdAndBedommelse_IsNotNull(Long utstillingId);

	List<Due> findByPaamelding_Utstilling_IdOrderByBurnummerAsc(@NonNull Long id);

	List<Due> findByBurnummerOrderByBurnummerAsc(Integer burnummer);

	@Query("SELECT d FROM Due d WHERE d.paamelding.utstilling.id = :utstillingId AND d.raseLookup IS NOT NULL AND d.burnummer = :burnummer AND CONCAT(d.raseLookup.navn, ' (', d.raseLookup.gruppe, ')') IN :raseFilter")
	Due finnDuePaameldtUtstillingMedBurnummerOgRiktigRase(@NonNull Long utstillingId, @NonNull Integer burnummer,
	                                                      @NonNull Collection<String> raseFilter);

	@Query("SELECT d FROM Due d WHERE d.paamelding.utstilling.id = :id AND d.raseLookup IS NOT NULL AND CONCAT(d.raseLookup.navn, ' (', d.raseLookup.gruppe, ')') IN :rases ORDER BY d.burnummer ASC")
	List<Due> findByPaamelding_Utstilling_IdAndRaseInIgnoreCaseOrderByBurnummerAsc(@NonNull Long id,
	                                                                               @NonNull Collection<String> rases);

	@Modifying
	@Query("UPDATE Due d SET d.raseLookup = :nyRase WHERE d.id IN :idListe")
	void updateRaseForIds(@Param("nyRase") @Nullable Rase nyRase, @Param("idListe") List<Long> idListe);

	@Modifying
	@Query("UPDATE Due d SET d.fargeLookup = :nyFarge WHERE d.id IN :idListe")
	void updateFargeForIds(@Param("nyFarge") @Nullable Farge nyFarge, @Param("idListe") List<Long> idListe);

	@Modifying
	@Query("UPDATE Due d SET d.variantLookup = :nyVariant WHERE d.id IN :idListe")
	void updateVariantForIds(@Param("nyVariant") @Nullable Variant nyVariant, @Param("idListe") List<Long> idListe);
}
