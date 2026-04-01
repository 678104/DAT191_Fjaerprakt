package no.hvl.peristeri.feature.due;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class DueServiceImpl implements DueService {
	private final DueRepository dueRepository;
	private final DueLookupService dueLookupService;

	@Override
	public Due leggTilDue(Due due) {
		return dueRepository.save(due);
	}

	@Override
	public Due oppdaterRingnummer(Long id, String lopenr, String aarstall) {
		Due due = dueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Due", id));
		due.setLopenummer(lopenr);
		due.setAarstall(aarstall);
		return dueRepository.save(due);
	}

	@Override
	public List<String> hentRaserPaameldtUtstilling(Long utstillingId) {
		return dueRepository.hentRaserPaameldtUtstilling(utstillingId);
	}

	/**
	 * Henter alle duer som er paameldt til en utstilling.<br>
	 * Duene er sortert etter burnummer, stigende.
	 *
	 * @param utstillingId Id til utstillingen.
	 *
	 * @return Liste med duer sortert etter burnummer, stigende.
	 */
	@Override
	public List<Due> finnAlleDuerPaameldtUTstilling(Long utstillingId) {
		return dueRepository.findByPaamelding_Utstilling_IdOrderByBurnummerAsc(utstillingId);
	}

	/**
	 * Henter alle duer som er paameldt til en utstilling og sorterer dem i henhold til en spesifisert rekkefølge.
	 *
	 * @param customOrder  Liste med raser i ønsket rekkefølge.
	 * @param utstillingId Id til utstillingen.
	 *
	 * @return Liste med duer sortert i henhold til den spesifiserte rekkefølgen.
	 */
	@Override
	public List<Due> findAllSortedByCustomOrder(List<String> customOrder, Long utstillingId) {
		List<Due> duer = dueRepository.findByPaamelding_Utstilling_Id(utstillingId);

		Map<String, Integer> orderMap = new HashMap<>();
		for (int i = 0; i < customOrder.size(); i++) {
			orderMap.put(customOrder.get(i), i);
		}

		duer.sort(Comparator.comparing(due ->
				orderMap.getOrDefault(due.getRase(), Integer.MAX_VALUE)));

		return duer;
	}


	@Override
	public List<Due> saveAll(List<Due> list) {
		return dueRepository.saveAll(list);
	}

	@Override
	public Due finnDueMedId(Long dueId) {
		return dueRepository.findById(dueId).orElseThrow(() -> new ResourceNotFoundException("Due", dueId));
	}

	@Override
	public Due oppdaterDueInfo(Long dueId, Long raseId, Long fargeId, Long variantId) {
		Due due = dueRepository.findById(dueId).orElseThrow(() -> new ResourceNotFoundException("Due", dueId));
		due.setRaseLookup(dueLookupService.finnRaseMedId(raseId));
		due.setFargeLookup(dueLookupService.finnFargeMedId(fargeId));
		due.setVariantLookup(dueLookupService.finnVariantMedId(variantId));
		return dueRepository.save(due);
	}

	@Transactional
	@Override
	public void endreRasePaDuer(Long nyRaseId, List<Long> dueIdListe) {
		dueRepository.updateRaseForIds(dueLookupService.finnRaseMedId(nyRaseId), dueIdListe);
	}

	@Transactional
	@Override
	public void endreFargePaDuer(Long nyFargeId, List<Long> dueIdListe) {
		dueRepository.updateFargeForIds(dueLookupService.finnFargeMedId(nyFargeId), dueIdListe);
	}

	@Transactional
	@Override
	public void endreVariantPaDuer(Long nyVariantId, List<Long> dueIdListe) {
		dueRepository.updateVariantForIds(dueLookupService.finnVariantMedId(nyVariantId), dueIdListe);
	}

}
