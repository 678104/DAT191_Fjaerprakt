package no.hvl.peristeri.feature.due;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class DueServiceImpl implements DueService {
	private final Logger logger = LoggerFactory.getLogger(DueServiceImpl.class);

	private final DueRepository dueRepository;

	@Override
	public Due leggTilDue(Due due) {
		return dueRepository.save(due);
	}

	@Override
	public Due oppdaterRingnummer(Long id, String lopenr, String aarstall) {
		validerRingnummer(lopenr, aarstall);
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
	public Due oppdaterDueInfo(Long dueId, String rase, String farge, String variant) {
		Due due = dueRepository.findById(dueId).orElseThrow(() -> new ResourceNotFoundException("Due", dueId));
		due.setRase(rase);
		due.setFarge(farge);
		due.setVariant(variant);
		return dueRepository.save(due);
	}


	@Transactional
	@Override
	public void endreRasePaDuer(String nyRase, List<Long> dueIdListe) {
		dueRepository.updateRaseForIds(nyRase, dueIdListe);
	}

	@Transactional
	@Override
	public void endreFargePaDuer(String nyFarge, List<Long> dueIdListe) {
		dueRepository.updateFargeForIds(nyFarge, dueIdListe);
	}

	@Transactional
	@Override
	public void endreVariantPaDuer(String nyVariant, List<Long> dueIdListe) {
		dueRepository.updateVariantForIds(nyVariant, dueIdListe);
	}

	private void validerRingnummer(String lopenr, String aarstall) {
		boolean tomLopenr = lopenr == null || lopenr.isBlank();
		boolean tomAarstall = aarstall == null || aarstall.isBlank();
		if (tomLopenr && tomAarstall) {
			return;
		}
		if (tomLopenr || tomAarstall) {
			throw new InvalidParameterException("ringnummer", "Både løpenummer og årstall må fylles ut.");
		}
		if (!lopenr.matches("\\d{1,10}")) {
			throw new InvalidParameterException("lopenr", "Løpenummer må bestå av tall.");
		}
		if (!aarstall.matches("\\d{2,4}")) {
			throw new InvalidParameterException("aarstall", "Årstall må bestå av 2-4 tall.");
		}
	}

}
