package no.hvl.peristeri.feature.utstilling;


import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.BusinessRuleViolationException;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.due.DueRepository;
import no.hvl.peristeri.feature.due.DueService;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.util.RaseStringHjelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UtstillingServiceImpl implements UtstillingService {
	private final Logger logger = LoggerFactory.getLogger(UtstillingServiceImpl.class);

	private final    UtstillingRepository utstillingRepository;
	private final    DueService           dueService;
	private final    DueRepository        dueRepository;
	private final    ReentrantLock        lock = new ReentrantLock();
	private volatile Long                 aktivUtstillingId;

	/**
	 * Henter den aktive utstillingen.<br>
	 * Hvis
	 *
	 * @return Den aktive utstillingen, eller null dersom ingen utstilling er aktiv.
	 */
	@Override
	public Utstilling finnAktivUtstilling() {
		Long id = aktivUtstillingId;

		// Double-checked locking for lazy init
		if (id == null) {
			lock.lock();
			try {
				if (aktivUtstillingId == null) {
					Optional<Utstilling> aktiv = utstillingRepository.finnAktivUtstilling();
					if (aktiv.isPresent()) {
						aktivUtstillingId = aktiv.get().getId();
						return aktiv.get();
					} else {
						return null;
					}
				} else {
					id = aktivUtstillingId;
				}
			} finally {
				lock.unlock();
			}
		}
		// Create a new final variable for the id to use in the lambda expression
		final Long finalId = id;
		return utstillingRepository.findById(finalId)
				.orElseThrow(() -> new ResourceNotFoundException("Utstilling", finalId));
	}

	/**
	 * Setter utstilling med id til aktiv.<br>
	 * Denne metoden oppdaterer også aktiv status i databasen.
	 *
	 * @param utstillingId Id til utstillingen som skal være aktiv
	 *
	 * @return Den nye aktiv utstillingen, eller null dersom utstilling med id ikke finnes.
	 */
	@Transactional
	@Override
	public Utstilling setAktivUtstilling(Long utstillingId) {
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}

		Utstilling utstilling = utstillingRepository.findById(utstillingId)
				.orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));

		lock.lock();
		try {
			utstillingRepository.deaktiverAktiveUtstillinger();

			utstilling.setAktiv(true);
			utstillingRepository.save(utstilling);
			aktivUtstillingId = utstillingId;
		} finally {
			lock.unlock();
		}
		logger.info("Aktivert utstilling med id: {}", utstillingId);
		return utstilling;
	}

	/**
	 * Fjerner den aktive utstillingen.<br>
	 * Denne metoden oppdaterer også aktiv status i databasen.
	 */
	@Transactional
	@Override
	public void fjernAktivUtstilling() {
		lock.lock();
		try {
			utstillingRepository.deaktiverAktiveUtstillinger();
			aktivUtstillingId = null;
		} finally {
			lock.unlock();
		}
	}


	@Override
	public Utstilling leggTilUtstilling(Utstilling nyUtstilling) {
		if (nyUtstilling == null) {
			throw new InvalidParameterException("nyUtstilling", "cannot be null");
		}
		nyUtstilling.setPaameldingAApnet(nyUtstilling.erPaameldingAapen(LocalDate.now()));
		Utstilling saved = utstillingRepository.save(nyUtstilling);
		logger.info("Lagret utstilling: {}", saved);
		return saved;
	}

	@Override
	public Utstilling oppdaterUtstilling(Long id, Utstilling oppdatertUtstilling) {
		logger.info("Ny utstilling info: {}", oppdatertUtstilling);
		if (id == null) {
			throw new InvalidParameterException("id", "cannot be null");
		}
		if (oppdatertUtstilling == null) {
			throw new InvalidParameterException("oppdatertUtstilling", "cannot be null");
		}
		if (oppdatertUtstilling.getId() == null) {
			oppdatertUtstilling.setId(id);
			logger.warn("Id i utstilling var null, setter id til {}", id);
		} else {
			if (!oppdatertUtstilling.getId().equals(id)) {
				throw new BusinessRuleViolationException("ID mismatch", "ID in the utstilling does not match the ID in the path");
			}
		}
		Optional<Utstilling> byId = utstillingRepository.findById(id);
		if (byId.isEmpty()) {
			throw new ResourceNotFoundException("Utstilling", id);
		}
		Utstilling existing = byId.get();
		existing.setAdresse(oppdatertUtstilling.getAdresse());
		existing.setArrangoer(oppdatertUtstilling.getArrangoer());
		existing.setDatoRange(oppdatertUtstilling.getDatoRange());
		existing.setPaameldingStartDato(oppdatertUtstilling.getPaameldingStartDato());
		if (oppdatertUtstilling.getPaameldingAApnet() != null) {
			existing.setManuellPaameldingStatus(oppdatertUtstilling.getPaameldingAApnet());
		}
		existing.setPaameldingsFrist(oppdatertUtstilling.getPaameldingsFrist());
		existing.setRedigeringsFrist(oppdatertUtstilling.getRedigeringsFrist());
		existing.setBeskrivelse(oppdatertUtstilling.getBeskrivelse());
		existing.setTittel(oppdatertUtstilling.getTittel());
		existing.setPaameldingAApnet(existing.erPaameldingAapen(LocalDate.now()));
		Utstilling saved = utstillingRepository.save(existing);
		logger.info("Oppdaterer utstilling: {}", saved);
		return saved;
	}

	@Override
	public Utstilling finnUtstillingMedId(Long id) {
		if (id == null) {
			throw new InvalidParameterException("id", "cannot be null");
		}
		Utstilling utstilling = utstillingRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Utstilling", id));
		utstilling.setPaameldingAApnet(utstilling.erPaameldingAapen(LocalDate.now()));
		return utstilling;
	}

	@Override
	public List<Utstilling> addAll(List<Utstilling> utstillinger) {
		return utstillingRepository.saveAll(utstillinger);
	}

	@Override
	public List<Utstilling> hentAlleUtstillinger() {
		List<Utstilling> utstillinger = utstillingRepository.findAll(Sort.by("datoRange.startDate").ascending());
		settEffektivPaameldingStatus(utstillinger);
		return utstillinger;
	}

	@Override
	public List<Utstilling> finnIkkeTidligereUtstillinger() {
		List<Utstilling> utstillinger = utstillingRepository.finnUtstillingerSomAvslutterEtterGittDato(LocalDate.now());
		settEffektivPaameldingStatus(utstillinger);
		return utstillinger;
	}

	@Override
	public List<Utstilling> finnUtstillingerMedMulighetForPaamelding() {
		List<Utstilling> utstillinger = utstillingRepository.finnUtstillingerMedAApenPaameldingSortertEtterStartdato();
		settEffektivPaameldingStatus(utstillinger);
		return utstillinger;
	}

	@Override
	public List<Utstilling> finnKommendeUtstillinger() {
		List<Utstilling> utstillinger = utstillingRepository.finnUtstillingerSomStarterEtterGittDato(LocalDate.now());
		settEffektivPaameldingStatus(utstillinger);
		return utstillinger;
	}

	@Override
	public List<Utstilling> finnTidligereUtstillinger() {
		List<Utstilling> utstillinger = utstillingRepository.finnUtstillingerSomEnderFoerGittDato(LocalDate.now());
		settEffektivPaameldingStatus(utstillinger);
		return utstillinger;
	}

	@Override
	public Utstilling oppdaterSorterteRaser(Long id, String raser) {
		if (id == null) {
			throw new InvalidParameterException("id", "cannot be null");
		}
		if (raser == null) {
			throw new InvalidParameterException("raser", "cannot be null");
		}

		Utstilling existing = utstillingRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Utstilling", id));
		existing.setRaseSortering(raser);
		Utstilling saved = utstillingRepository.save(existing);
		logger.info("Utstilling med id {} bruker nå denne sortering på raser: {}", id, saved.getRaseSortering());
		return saved;
	}

	@Transactional
	@Override
	public Utstilling genererBurnumre(Utstilling utstilling) {
		List<String> sortering = RaseStringHjelper.konverterTilListe(utstilling.getRaseSortering());
		List<Due>    sorted    = dueService.findAllSortedByCustomOrder(sortering, utstilling.getId());

		for (int i = 0; i < sorted.size(); i++) {
			Due due = sorted.get(i);
			due.setBurnummer(i + 1);
		}
		dueService.saveAll(sorted);
		logger.info("Genererte burnumre for utstilling med id {}", utstilling.getId());
		utstilling.setHarBurnumre(true);

		return utstilling;
	}

	@Override
	public List<Due> finnAlleDuerFraUtstillingSomHarBedommelse(Long id) {
		return dueRepository.findByPaamelding_Utstilling_IdAndBedommelse_IsNotNull(id);
	}

	@Override
	public Map<String, List<String>> hentFargeForRaseFraUtstilling(Utstilling utstilling) {
		List<String>              raser         = dueService.hentRaserPaameldtUtstilling(utstilling.getId());
		Map<String, List<String>> raseFargerMap = new HashMap<>();
		for (String rase : raser) {
			List<String> farger = utstilling.getPaameldinger().stream()
			                                .flatMap(p -> p.getDuer().stream())
			                                .filter(d -> rase.equals(d.getRase()) && d.getBedommelse() != null)
			                                .map(Due::getFarge)
			                                .distinct()
			                                .sorted()
			                                .toList();

			raseFargerMap.put(rase, farger);
		}

		return raseFargerMap;
	}

	/**
	 * Henter status på bedømmelser for en utstilling.<br>
	 * Status er antall duer påmeldt og antall duer som er blitt bedømt.<br>
	 *
	 * @param utstilling Utstillingen som skal sjekkes.
	 *
	 * @return Liste med 2 elementer, der første element er antall duer påmeldt og andre element er antall duer som er blitt bedømt.
	 */
	@Override
	public List<Long> hentStatusPaaBedommelser(Utstilling utstilling) {
		return utstilling.getPaameldinger()
		                 .stream()
		                 .flatMap(p -> p.getDuer().stream())
		                 .collect(Collectors.teeing(Collectors.counting(),
				                 Collectors.filtering(
						                 d -> d.getBedommelse() != null && d.getBedommelse().getPoeng() != null,
						                 Collectors.counting()),
				                 List::of));
	}

	@Override
	public List<Bruker> hentSortertListeAvUtstillereFraUtstilling(Long utstillingId) {
		Utstilling utstilling = utstillingRepository.findById(utstillingId)
		                                            .orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));

		return utstilling.getPaameldinger().stream()
		                 .map(Paamelding::getUtstiller)
		                 .distinct()
		                 .sorted(Comparator.comparing(Bruker::getEtternavn)
		                                   .thenComparing(Bruker::getFornavn))
		                 .collect(Collectors.toList());
	}

	@Override
	public Map<Long, List<String>> hentUtstillereSineRaser(Long utstillingId) {
		Utstilling utstilling = utstillingRepository.findById(utstillingId)
		                                            .orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));

		Map<Long, List<String>> utstillerRaserMap = new HashMap<>();

		utstilling.getPaameldinger().forEach(paamelding -> {
			Long brukerId = paamelding.getUtstiller().getId();

			List<String> raser = paamelding.getDuer().stream()
			                               .map(Due::getRase)
			                               .distinct()
			                               .sorted()
			                               .collect(Collectors.toList());

			utstillerRaserMap.put(brukerId, raser);
		});

		return utstillerRaserMap;
	}

	@Override
	public Map<String, List<String>> hentVarianterForRase(Long utstillingId) {
		Utstilling utstilling = utstillingRepository.findById(utstillingId)
		                                            .orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));

		Map<String, List<String>> raseVarianterMap = new HashMap<>();

		// Get all unique races from the exhibition
		List<String> raser = dueService.hentRaserPaameldtUtstilling(utstillingId);

		// For each race, find all distinct variants
		for (String rase : raser) {
			List<String> varianter = utstilling.getPaameldinger().stream()
			                                   .flatMap(paamelding -> paamelding.getDuer().stream())
			                                   .filter(due -> rase.equals(due.getRase()))
			                                   .map(Due::getVariant)
			                                   .filter(Objects::nonNull)
			                                   .distinct()
			                                   .sorted()
			                                   .collect(Collectors.toList());

			raseVarianterMap.put(rase, varianter);
		}

		return raseVarianterMap;
	}

	@Override
	public Map<String, List<String>> hentFargerForVarianter(Long utstillingId) {
		Utstilling utstilling = utstillingRepository.findById(utstillingId)
		                                            .orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));

		Map<String, List<String>> variantFargerMap = new HashMap<>();

		// Get all unique variants from the exhibition
		List<Due> duer = utstilling.getPaameldinger().stream()
		                           .flatMap(paamelding -> paamelding.getDuer().stream())
		                           .toList();

		// For each variant, find all distinct colors
		duer.stream()
		    .filter(due -> due.getVariant() != null)
		    .map(Due::getVariant)
		    .distinct()
		    .forEach(variant -> {
			    List<String> farger = duer.stream()
			                              .filter(due -> variant.equals(due.getVariant()))
			                              .map(Due::getFarge)
			                              .filter(Objects::nonNull)
			                              .distinct()
			                              .sorted()
			                              .collect(Collectors.toList());

			    variantFargerMap.put(variant, farger);
		    });

		return variantFargerMap;
	}

	private void settEffektivPaameldingStatus(List<Utstilling> utstillinger) {
		LocalDate iDag = LocalDate.now();
		for (Utstilling utstilling : utstillinger) {
			utstilling.setPaameldingAApnet(utstilling.erPaameldingAapen(iDag));
		}
	}
}
