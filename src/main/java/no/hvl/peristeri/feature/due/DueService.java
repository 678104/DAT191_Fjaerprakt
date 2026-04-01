package no.hvl.peristeri.feature.due;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DueService {
	Due leggTilDue(Due due);

	Due oppdaterRingnummer(Long id, String lopenr, String aarstall);

	List<String> hentRaserPaameldtUtstilling(Long utstillingId);

	/**
	 * Henter alle duer som er paameldt til en utstilling.<br>
	 * Duene er sortert etter burnummer, stigende.
	 *
	 * @param utstillingId Id til utstillingen.
	 *
	 * @return Liste med duer sortert etter burnummer, stigende.
	 */
	List<Due> finnAlleDuerPaameldtUTstilling(Long utstillingId);

	/**
	 * Henter alle duer som er paameldt til en utstilling og sorterer dem i henhold til en spesifisert rekkefølge.
	 *
	 * @param customOrder  Liste med raser i ønsket rekkefølge.
	 * @param utstillingId Id til utstillingen.
	 *
	 * @return Liste med duer sortert i henhold til den spesifiserte rekkefølgen.
	 */
	List<Due> findAllSortedByCustomOrder(List<String> customOrder, Long utstillingId);

	List<Due> saveAll(List<Due> list);

	Due finnDueMedId(Long dueId);

	Due oppdaterDueInfo(Long dueId, Long raseId, Long fargeId, Long variantId);

	@Transactional
	void endreRasePaDuer(Long nyRaseId, List<Long> dueIdListe);

	@Transactional
	void endreFargePaDuer(Long nyFargeId, List<Long> dueIdListe);

	@Transactional
	void endreVariantPaDuer(Long nyVariantId, List<Long> dueIdListe);
}
