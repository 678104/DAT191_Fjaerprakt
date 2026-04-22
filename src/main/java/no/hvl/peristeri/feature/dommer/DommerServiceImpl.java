package no.hvl.peristeri.feature.dommer;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import no.hvl.peristeri.feature.bruker.Rolle;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.due.DueNotFoundException;
import no.hvl.peristeri.feature.due.DueRepository;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingRepository;
import no.hvl.peristeri.util.RaseStringHjelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DommerServiceImpl implements DommerService {

	private final DueRepository              dueRepo;
	private final BedommelseRepository       bedommelseRepo;
	private final BedommelseBildeRepository  bedommelseBildeRepository;
	private final UtstillingRepository       utstillingRepo;
	private final DommerPaameldingRepository dommerPaameldingRepository;
	private final DommerVinnerRepository     dommerVinnerRepository;
	private final BrukerService              brukerService;

	private static final Comparator<Due> DUE_VINNER_SORTERING =
			Comparator.comparing((Due due) -> due.getBedommelse() != null ? due.getBedommelse().getPoeng() : 0,
					Comparator.nullsLast(Integer::compareTo))
					.reversed()
					.thenComparing(due -> due.getBurnummer() == null ? Integer.MAX_VALUE : due.getBurnummer());
	private static final String NORGESMESTER_OPPDRETT_1 = "NORGESMESTER_OPPDRETT_1";
	private static final String NORGESMESTER_OPPDRETT_2 = "NORGESMESTER_OPPDRETT_2";
	private static final String NORGESMESTER_OPPDRETT_3 = "NORGESMESTER_OPPDRETT_3";

	@Override
	public List<Due> hentAlleDuer() {
		List<Due> liste = dueRepo.findAll();
		System.out.println("Fant " + liste.size() + " duer i databasen.");
		return liste;
	}

	@Override
	@Transactional
	public void lagreBedommelse(Long dueId, Bedommelse nyBedommelse, Bruker dommer) {
		lagreBedommelseIntern(dueId, nyBedommelse, dommer, null);
	}

	@Override
	@Transactional
	public void lagreBedommelse(Long dueId, Bedommelse nyBedommelse, Bruker dommer, Long utstillingId) {
		lagreBedommelseIntern(dueId, nyBedommelse, dommer, utstillingId);
	}

	private void lagreBedommelseIntern(Long dueId, Bedommelse nyBedommelse, Bruker dommer, Long utstillingId) {
		if (dueId == null) {
			throw new InvalidParameterException("dueId", "cannot be null");
		}
		if (nyBedommelse == null) {
			throw new InvalidParameterException("nyBedommelse", "cannot be null");
		}
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}

		DommerPaamelding dp = finnDommerPaameldingForUtstilling(dommer, utstillingId);

		Due due = hentDueMedId(dueId);
		Long dueUtstillingId = due.getPaamelding() != null && due.getPaamelding().getUtstilling() != null
				? due.getPaamelding().getUtstilling().getId()
				: null;
		if (dueUtstillingId == null || !dueUtstillingId.equals(dp.getUtstilling().getId())) {
			throw new ResourceNotFoundException("Due",
					"Due med id " + dueId + " er ikke paameldt valgt utstilling");
		}

		Bedommelse eksisterende = due.getBedommelse();

		if (eksisterende != null) {
			// Oppdater feltene på den eksisterende bedømmelsen
			eksisterende.setPoeng(nyBedommelse.getPoeng());
			eksisterende.setFordeler(nyBedommelse.getFordeler());
			eksisterende.setOnsker(nyBedommelse.getOnsker());
			eksisterende.setFeil(nyBedommelse.getFeil());
			eksisterende.setKategorier(nyBedommelse.getKategorier());
			eksisterende.setBedommelsesTidspunkt(nyBedommelse.getBedommelsesTidspunkt());
			eksisterende.setBedomtAv(dp);
			handterBildeOppdatering(due, eksisterende, nyBedommelse);

			bedommelseRepo.save(eksisterende);
		} else {
			// Ny bedømmelse
			nyBedommelse.setDue(due);
			due.setBedommelse(nyBedommelse);

			nyBedommelse.setBedomtAv(dp);
			handterBildeOppdatering(due, nyBedommelse, nyBedommelse);

			bedommelseRepo.save(nyBedommelse);
			dueRepo.save(due);
		}
	}

	private void handterBildeOppdatering(Due due, Bedommelse target, Bedommelse incoming) {
		if (incoming.isFjernBilde()) {
			BedommelseBilde eksisterendeBilde = bedommelseBildeRepository.findByBedommelse_Due_Id(due.getId()).orElse(null);
			if (eksisterendeBilde != null) {
				bedommelseBildeRepository.delete(eksisterendeBilde);
			}
			target.setBilde(null);
			return;
		}

		BedommelseBilde nyttBilde = incoming.getBilde();
		if (nyttBilde == null) {
			return;
		}

		BedommelseBilde eksisterendeBilde = bedommelseBildeRepository.findByBedommelse_Due_Id(due.getId()).orElse(null);
		if (eksisterendeBilde != null) {
			eksisterendeBilde.setContentType(nyttBilde.getContentType());
			eksisterendeBilde.setFilnavn(nyttBilde.getFilnavn());
			eksisterendeBilde.setSizeBytes(nyttBilde.getSizeBytes());
			eksisterendeBilde.setData(nyttBilde.getData());
			bedommelseBildeRepository.save(eksisterendeBilde);
			return;
		}

		target.setBilde(nyttBilde);
	}

	@Override
	public List<Due> finnDuerEtterBurnummer(Integer burnummer) {
		if (burnummer == null) {
			throw new InvalidParameterException("burnummer", "cannot be null");
		}
		return dueRepo.findByBurnummerOrderByBurnummerAsc(burnummer);
	}

	@Override
	public List<DommerPaamelding> finnDommerPaameldingerTilUtstilling(Long utstillingId) {
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}
		return dommerPaameldingRepository.finnPaameldingerEtterUtstillingId(utstillingId);
	}

	@Override
	public DommerPaamelding lagreDommerPaamelding(Bruker dommer, Long utstillingId, String passord) {
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}

		dommer.leggTilRolle(Rolle.DOMMER);
		Bruker saved = brukerService.lagreBrukerMedPassord(dommer, passord);
		if (dommerPaameldingRepository.existsByDommer_IdAndUtstilling_Id(saved.getId(), utstillingId)) {
			return dommerPaameldingRepository.findByDommer_IdAndUtstilling_IdOrderByIdAsc(saved.getId(), utstillingId)
					.getFirst();
		}
		Utstilling utstilling = utstillingRepo.findById(utstillingId)
		                                      .orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));
		DommerPaamelding dommerPaamelding = new DommerPaamelding(utstilling, saved);
		return dommerPaameldingRepository.save(dommerPaamelding);
	}

	@Override
	public List<Bruker> hentDommere() {
		return brukerService.hentBrukereMedRolle(Rolle.DOMMER);
	}

	@Override
	@Transactional
	public Bruker tildelDommerRolle(Long brukerId) {
		return brukerService.leggTilRolle(brukerId, Rolle.DOMMER);
	}

	@Override
	@Transactional
	public void fjernDommerRolle(Long brukerId) {
		if (brukerId == null) {
			throw new InvalidParameterException("brukerId", "cannot be null");
		}

		List<DommerPaamelding> paameldinger = dommerPaameldingRepository.finnPaameldingerEtterDommerId(brukerId);
		for (DommerPaamelding dp : paameldinger) {
			List<Due> duer = dueRepo.findByTildeltDommer_Id(dp.getId());
			for (Due due : duer) {
				due.setTildeltDommer(null);
			}
			if (!duer.isEmpty()) {
				dueRepo.saveAll(duer);
			}

			List<Bedommelse> bedommelser = bedommelseRepo.findByBedomtAv_Id(dp.getId());
			for (Bedommelse bedommelse : bedommelser) {
				bedommelse.setBedomtAv(null);
			}
			if (!bedommelser.isEmpty()) {
				bedommelseRepo.saveAll(bedommelser);
			}

			dommerPaameldingRepository.delete(dp);
		}

		brukerService.fjernRolle(brukerId, Rolle.DOMMER);
	}

	@Override
	@Transactional
	public void tildelDommerTilUtstillinger(Long brukerId, List<Long> utstillingIder) {
		if (brukerId == null) {
			throw new InvalidParameterException("brukerId", "cannot be null");
		}
		Bruker dommer = brukerService.leggTilRolle(brukerId, Rolle.DOMMER);
		Set<Long> valgteUtstillinger = utstillingIder == null
				? Set.of()
				: utstillingIder.stream().filter(java.util.Objects::nonNull).collect(Collectors.toCollection(HashSet::new));

		for (Long utstillingId : valgteUtstillinger) {
			if (!dommerPaameldingRepository.existsByDommer_IdAndUtstilling_Id(dommer.getId(), utstillingId)) {
				Utstilling utstilling = utstillingRepo.findById(utstillingId)
						.orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));
				dommerPaameldingRepository.save(new DommerPaamelding(utstilling, dommer));
			}
		}
	}

	@Override
	public List<Utstilling> hentUtstillingerForDommer(Long brukerId) {
		if (brukerId == null) {
			throw new InvalidParameterException("brukerId", "cannot be null");
		}
		return dommerPaameldingRepository.finnPaameldingerEtterDommerId(brukerId).stream()
				.map(DommerPaamelding::getUtstilling)
				.toList();
	}

	@Override
	public DommerVinnerData hentVinnerData(Bruker dommer, Long utstillingId) {
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}

		List<Due> alleTildelteDuer = finnDuerDommerSkalBedomme(dommer, utstillingId);
		if (alleTildelteDuer.isEmpty()) {
			return new DommerVinnerData(false, Map.of(), Map.of(), List.of(), Map.of(), Map.of(), null, null, null, null);
		}

		boolean klarForVinnerkaring = alleTildelteDuer.stream().allMatch(this::harPoeng);
		if (!klarForVinnerkaring) {
			return new DommerVinnerData(false, Map.of(), Map.of(), List.of(), Map.of(), Map.of(), null, null, null, null);
		}

		List<Due> kandidater = alleTildelteDuer.stream()
				.filter(this::harPoeng)
				.sorted(DUE_VINNER_SORTERING)
				.toList();

		Map<String, List<Due>> kandidaterPerRase = grupperEtterRase(kandidater);
		Map<String, List<Due>> kandidaterPerGruppe = grupperEtterGruppe(kandidater);

		DommerPaamelding dp = finnDommerPaameldingForUtstilling(dommer, utstillingId);
		List<DommerVinner> eksisterendeVinnere = dommerVinnerRepository
				.findByDommerPaamelding_IdOrderByTypeAscKategoriNavnAsc(dp.getId());

		Map<String, Long> valgteRasevinnere = new LinkedHashMap<>();
		Map<String, Long> valgteGruppevinnere = new LinkedHashMap<>();
		Long valgtBisVinnerId = null;
		Long valgtNorgesmesterOppdrett1Id = null;
		Long valgtNorgesmesterOppdrett2Id = null;
		Long valgtNorgesmesterOppdrett3Id = null;

		for (DommerVinner vinner : eksisterendeVinnere) {
			if (vinner.getType() == DommerVinnerType.RASE) {
				valgteRasevinnere.put(vinner.getKategoriNavn(), vinner.getDue().getId());
			} else if (vinner.getType() == DommerVinnerType.GRUPPE) {
				valgteGruppevinnere.put(vinner.getKategoriNavn(), vinner.getDue().getId());
			} else if (vinner.getType() == DommerVinnerType.BIS) {
				if ("BIS".equals(vinner.getKategoriNavn())) {
					valgtBisVinnerId = vinner.getDue().getId();
				} else if (NORGESMESTER_OPPDRETT_1.equals(vinner.getKategoriNavn())) {
					valgtNorgesmesterOppdrett1Id = vinner.getDue().getId();
				} else if (NORGESMESTER_OPPDRETT_2.equals(vinner.getKategoriNavn())) {
					valgtNorgesmesterOppdrett2Id = vinner.getDue().getId();
				} else if (NORGESMESTER_OPPDRETT_3.equals(vinner.getKategoriNavn())) {
					valgtNorgesmesterOppdrett3Id = vinner.getDue().getId();
				}
			}
		}

		return new DommerVinnerData(
				true,
				kandidaterPerRase,
				kandidaterPerGruppe,
				kandidater,
				valgteRasevinnere,
				valgteGruppevinnere,
				valgtBisVinnerId,
				valgtNorgesmesterOppdrett1Id,
				valgtNorgesmesterOppdrett2Id,
				valgtNorgesmesterOppdrett3Id
		);
	}

	@Override
	@Transactional
	public void lagreVinnere(Bruker dommer,
	                        Long utstillingId,
	                        List<String> raseNavn,
	                        List<Long> raseVinnerDueId,
	                        List<String> gruppeNavn,
	                        List<Long> gruppeVinnerDueId,
	                        Long bisVinnerDueId,
	                        Long norgesmesterOppdrett1DueId,
	                        Long norgesmesterOppdrett2DueId,
	                        Long norgesmesterOppdrett3DueId) {
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}

		List<Due> alleTildelteDuer = finnDuerDommerSkalBedomme(dommer, utstillingId);
		if (alleTildelteDuer.isEmpty() || alleTildelteDuer.stream().anyMatch(due -> !harPoeng(due))) {
			throw new InvalidParameterException("vinnere", "Alle duer må være bedømt før vinnerkåring.");
		}

		List<Due> kandidater = alleTildelteDuer.stream().filter(this::harPoeng).toList();
		Map<Long, Due> kandidaterPerId = kandidater.stream().collect(Collectors.toMap(Due::getId, due -> due));
		Map<String, List<Due>> kandidaterPerRase = grupperEtterRase(kandidater);
		Map<String, List<Due>> kandidaterPerGruppe = grupperEtterGruppe(kandidater);

		if (!harLikLengde(raseNavn, raseVinnerDueId) || !harLikLengde(gruppeNavn, gruppeVinnerDueId)) {
			throw new InvalidParameterException("vinnere", "Ugyldig vinnerdata sendt inn.");
		}

		DommerPaamelding dp = finnDommerPaameldingForUtstilling(dommer, utstillingId);
		dommerVinnerRepository.deleteByDommerPaamelding_Id(dp.getId());

		if (raseNavn != null && raseVinnerDueId != null) {
			for (int i = 0; i < raseNavn.size(); i++) {
				String rase = trimTilTomTekst(raseNavn.get(i));
				Long dueId = raseVinnerDueId.get(i);
				Due valgtDue = validerOgFinnValgtDue(kandidaterPerId, kandidaterPerRase.getOrDefault(rase, List.of()), dueId,
						"Ugyldig rasevinner for " + rase);
				dommerVinnerRepository.save(lagVinner(dp, valgtDue, DommerVinnerType.RASE, rase));
			}
		}

		if (gruppeNavn != null && gruppeVinnerDueId != null) {
			for (int i = 0; i < gruppeNavn.size(); i++) {
				String gruppe = trimTilTomTekst(gruppeNavn.get(i));
				Long dueId = gruppeVinnerDueId.get(i);
				Due valgtDue = validerOgFinnValgtDue(kandidaterPerId, kandidaterPerGruppe.getOrDefault(gruppe, List.of()), dueId,
						"Ugyldig gruppevinner for " + gruppe);
				dommerVinnerRepository.save(lagVinner(dp, valgtDue, DommerVinnerType.GRUPPE, gruppe));
			}
		}

		if (bisVinnerDueId != null) {
			Due bisDue = kandidaterPerId.get(bisVinnerDueId);
			if (bisDue == null) {
				throw new InvalidParameterException("bisVinnerDueId", "Ugyldig BIS-vinner.");
			}
			dommerVinnerRepository.save(lagVinner(dp, bisDue, DommerVinnerType.BIS, "BIS"));
		}

		lagreNorgesmesterOppdrett(dp,
				kandidaterPerId,
				norgesmesterOppdrett1DueId,
				norgesmesterOppdrett2DueId,
				norgesmesterOppdrett3DueId);
	}

	@Override
	public DommerPaamelding fordelRaserTilDommer(Long dommerPaameldingId, List<String> raser) {
		if (dommerPaameldingId == null) {
			throw new InvalidParameterException("dommerPaameldingId", "cannot be null");
		}
		if (raser == null) {
			throw new InvalidParameterException("raser", "cannot be null");
		}

		DommerPaamelding dp = dommerPaameldingRepository.findById(dommerPaameldingId)
				.orElseThrow(() -> new ResourceNotFoundException("DommerPaamelding", dommerPaameldingId));
		RaseStringHjelper.settInnRaser(dp, raser);
		return dommerPaameldingRepository.save(dp);
	}

	@Override
	public Due tilordneDueTilDommer(Long dommerPaameldingId, Long dueId) {
		if (dommerPaameldingId == null) {
			throw new InvalidParameterException("dommerPaameldingId", "cannot be null");
		}
		if (dueId == null) {
			throw new InvalidParameterException("dueId", "cannot be null");
		}

		DommerPaamelding dp = dommerPaameldingRepository.findById(dommerPaameldingId)
				.orElseThrow(() -> new ResourceNotFoundException("DommerPaamelding", dommerPaameldingId));
		Due due = hentDueMedId(dueId);

		if (due.getPaamelding() == null || due.getPaamelding().getUtstilling() == null
				|| !due.getPaamelding().getUtstilling().getId().equals(dp.getUtstilling().getId())) {
			throw new ResourceNotFoundException("Due", "Due med id " + dueId + " er ikke paameldt denne utstillingen");
		}

		due.setTildeltDommer(dp);
		return dueRepo.save(due);
	}

	@Override
	public List<Due> tilordneDuerTilDommer(Long dommerPaameldingId, List<Long> dueIder) {
		if (dueIder == null || dueIder.isEmpty()) {
			throw new InvalidParameterException("dueIder", "cannot be null or empty");
		}
		return dueIder.stream()
				.distinct()
				.map(dueId -> tilordneDueTilDommer(dommerPaameldingId, dueId))
				.toList();
	}

	@Override
	public List<Due> tilordneDuerTilDommerEtterRaser(Long dommerPaameldingId, Long utstillingId, List<String> raser) {
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}
		if (raser == null || raser.isEmpty()) {
			throw new InvalidParameterException("raser", "cannot be null or empty");
		}

		DommerPaamelding dp = dommerPaameldingRepository.findById(dommerPaameldingId)
				.orElseThrow(() -> new ResourceNotFoundException("DommerPaamelding", dommerPaameldingId));

		List<Due> duer = dueRepo.findByPaamelding_Utstilling_IdOrderByBurnummerAsc(utstillingId).stream()
				.filter(due -> due.getRase() != null)
				.filter(due -> raser.stream().anyMatch(rase -> rase.equalsIgnoreCase(due.getRase())))
				.toList();

		duer.forEach(due -> due.setTildeltDommer(dp));
		return dueRepo.saveAll(duer);
	}

	@Override
	public Due hentDueMedId(Long dueId) {
		return dueRepo.findById(dueId).orElseThrow(() -> new DueNotFoundException(dueId));
	}

	@Override
	public List<DommerPaamelding> finnDommerPaameldinger(Bruker dommer) {
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}
		return dommerPaameldingRepository.finnPaameldingerEtterDommerId(dommer.getId());
	}

	@Override
	public List<Due> finnDuerDommerSkalBedomme(Bruker dommer) {
		DommerPaamelding paamelding;
		try {
			paamelding = finnDommerPaameldingForUtstilling(dommer, null);
		} catch (ResourceNotFoundException ignored) {
			return List.of();
		}
		List<String> raser = RaseStringHjelper.hentUtRaser(paamelding);
		return dueRepo.findByPaamelding_Utstilling_IdAndRaseInIgnoreCaseOrderByBurnummerAsc(
				paamelding.getUtstilling().getId(), raser);
	}

	@Override
	public List<Due> finnDuerDommerSkalBedomme(Bruker dommer, Long utstillingId) {
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}
		DommerPaamelding paamelding = finnDommerPaameldingForUtstilling(dommer, utstillingId);
		List<String> raser = RaseStringHjelper.hentUtRaser(paamelding);
		return dueRepo.findByPaamelding_Utstilling_IdAndRaseInIgnoreCaseOrderByBurnummerAsc(
				paamelding.getUtstilling().getId(), raser);
	}

	@Override
	public DommerPaamelding lagreDommerPaaMelding(DommerPaamelding dommerPaamelding) {
		if (dommerPaamelding == null) {
			throw new InvalidParameterException("dommerPaamelding", "cannot be null");
		}
		return dommerPaameldingRepository.save(dommerPaamelding);
	}

	@Override
	public void fjernDommerPaamelding(Long dommerPaameldingId) {
		if (dommerPaameldingId == null) {
			throw new InvalidParameterException("dommerPaameldingId", "cannot be null");
		}
		DommerPaamelding dp = dommerPaameldingRepository.findById(dommerPaameldingId)
				.orElseThrow(() -> new ResourceNotFoundException("DommerPaamelding", dommerPaameldingId));
		List<Due> duer = dueRepo.findByTildeltDommer_Id(dp.getId());
		for (Due due : duer) {
			due.setTildeltDommer(null);
		}
		if (!duer.isEmpty()) {
			dueRepo.saveAll(duer);
		}
		List<Bedommelse> bedommelser = bedommelseRepo.findByBedomtAv_Id(dp.getId());
		for (Bedommelse bedommelse : bedommelser) {
			bedommelse.setBedomtAv(null);
		}
		if (!bedommelser.isEmpty()) {
			bedommelseRepo.saveAll(bedommelser);
		}
		dommerPaameldingRepository.delete(dp);
	}

	@Override
	public Due finnDueDommerSkalBedommeMedBurnummer(Bruker dommer, Integer burnummer) {
		if (burnummer == null) {
			throw new InvalidParameterException("burnummer", "cannot be null");
		}
		DommerPaamelding paamelding = finnDommerPaameldingForUtstilling(dommer, null);
		List<String> raser = RaseStringHjelper.hentUtRaser(paamelding);
		Due due = dueRepo.finnDuePaameldtUtstillingMedBurnummerOgRiktigRase(paamelding.getUtstilling().getId(), burnummer, raser);
		if (due == null) {
			throw new ResourceNotFoundException("Due", "with burnummer " + burnummer);
		}
		return due;
	}

	@Override
	public Due finnDueDommerSkalBedommeMedBurnummer(Bruker dommer, Integer burnummer, Long utstillingId) {
		if (burnummer == null) {
			throw new InvalidParameterException("burnummer", "cannot be null");
		}
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}
		DommerPaamelding paamelding = finnDommerPaameldingForUtstilling(dommer, utstillingId);
		List<String> raser = RaseStringHjelper.hentUtRaser(paamelding);
		Due due = dueRepo.finnDuePaameldtUtstillingMedBurnummerOgRiktigRase(paamelding.getUtstilling().getId(), burnummer, raser);
		if (due == null) {
			throw new ResourceNotFoundException("Due", "with burnummer " + burnummer);
		}
		return due;
	}

	private DommerPaamelding finnDommerPaameldingForUtstilling(Bruker dommer, Long utstillingId) {
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}
		if (utstillingId == null) {
			return dommerPaameldingRepository.finnPaameldingForAktivUtstilling(dommer.getId())
					.orElseThrow(() -> new ResourceNotFoundException("DommerPaamelding", "Ingen aktiv utstilling funnet for dommer"));
		}
		List<DommerPaamelding> treff = dommerPaameldingRepository.findByDommer_IdAndUtstilling_IdOrderByIdAsc(dommer.getId(), utstillingId);
		if (treff.isEmpty()) {
			throw new ResourceNotFoundException("DommerPaamelding",
					"Dommer er ikke tildelt utstilling med id " + utstillingId);
		}
		return treff.getFirst();
	}

	private Map<String, List<Due>> grupperEtterRase(List<Due> duer) {
		return duer.stream()
				.collect(Collectors.groupingBy(
						due -> trimTilTomTekst(due.getRase()).isBlank() ? "Ukjent rase" : due.getRase().trim(),
						LinkedHashMap::new,
						Collectors.collectingAndThen(Collectors.toList(),
								liste -> liste.stream().sorted(DUE_VINNER_SORTERING).toList())
				));
	}

	private Map<String, List<Due>> grupperEtterGruppe(List<Due> duer) {
		return duer.stream()
				.collect(Collectors.groupingBy(
						due -> finnGruppeNavn(due.getRase()),
						LinkedHashMap::new,
						Collectors.collectingAndThen(Collectors.toList(),
								liste -> liste.stream().sorted(DUE_VINNER_SORTERING).toList())
				));
	}

	private String finnGruppeNavn(String rase) {
		String verdi = trimTilTomTekst(rase);
		if (verdi.isBlank()) {
			return "Ukjent gruppe";
		}

		int startParentes = verdi.indexOf('(');
		int sluttParentes = verdi.indexOf(')');
		if (startParentes >= 0 && sluttParentes > startParentes) {
			String parentesVerdi = trimTilTomTekst(verdi.substring(startParentes + 1, sluttParentes));
			if (!parentesVerdi.isBlank()) {
				return parentesVerdi;
			}
		}

		if (verdi.contains("-")) {
			return trimTilTomTekst(verdi.substring(0, verdi.indexOf('-')));
		}
		if (verdi.contains(":")) {
			return trimTilTomTekst(verdi.substring(0, verdi.indexOf(':')));
		}

		String[] deler = verdi.split("\\s+");
		return deler.length == 0 ? "Ukjent gruppe" : deler[0];
	}

	private boolean harPoeng(Due due) {
		return due != null
				&& due.getBedommelse() != null
				&& due.getBedommelse().getPoeng() != null;
	}

	private boolean harLikLengde(List<?> a, List<?> b) {
		int aLen = a == null ? 0 : a.size();
		int bLen = b == null ? 0 : b.size();
		return aLen == bLen;
	}

	private Due validerOgFinnValgtDue(Map<Long, Due> kandidaterPerId, List<Due> kategoriKandidater, Long dueId, String feilmelding) {
		if (dueId == null) {
			throw new InvalidParameterException("dueId", feilmelding);
		}
		Due valgt = kandidaterPerId.get(dueId);
		if (valgt == null) {
			throw new InvalidParameterException("dueId", feilmelding);
		}
		boolean finnesIKategori = kategoriKandidater.stream().map(Due::getId).filter(Objects::nonNull).anyMatch(id -> id.equals(dueId));
		if (!finnesIKategori) {
			throw new InvalidParameterException("dueId", feilmelding);
		}
		return valgt;
	}

	private void lagreNorgesmesterOppdrett(DommerPaamelding dp,
	                                      Map<Long, Due> kandidaterPerId,
	                                      Long plass1DueId,
	                                      Long plass2DueId,
	                                      Long plass3DueId) {
		if (plass1DueId == null && plass2DueId == null && plass3DueId == null) {
			return;
		}
		if (plass1DueId == null || plass2DueId == null || plass3DueId == null) {
			throw new InvalidParameterException("norgesmesterOppdrett", "Velg 1., 2. og 3. plass for Norgesmester i oppdrett.");
		}
		Set<Long> unike = new HashSet<>();
		unike.add(plass1DueId);
		unike.add(plass2DueId);
		unike.add(plass3DueId);
		if (unike.size() != 3) {
			throw new InvalidParameterException("norgesmesterOppdrett", "1., 2. og 3. plass må være ulike duer.");
		}

		Due plass1 = kandidaterPerId.get(plass1DueId);
		Due plass2 = kandidaterPerId.get(plass2DueId);
		Due plass3 = kandidaterPerId.get(plass3DueId);
		if (plass1 == null || plass2 == null || plass3 == null) {
			throw new InvalidParameterException("norgesmesterOppdrett", "Ugyldig valg for Norgesmester i oppdrett.");
		}

		dommerVinnerRepository.save(lagVinner(dp, plass1, DommerVinnerType.BIS, NORGESMESTER_OPPDRETT_1));
		dommerVinnerRepository.save(lagVinner(dp, plass2, DommerVinnerType.BIS, NORGESMESTER_OPPDRETT_2));
		dommerVinnerRepository.save(lagVinner(dp, plass3, DommerVinnerType.BIS, NORGESMESTER_OPPDRETT_3));
	}

	private DommerVinner lagVinner(DommerPaamelding dp, Due due, DommerVinnerType type, String kategoriNavn) {
		DommerVinner vinner = new DommerVinner();
		vinner.setDommerPaamelding(dp);
		vinner.setDue(due);
		vinner.setType(type);
		vinner.setKategoriNavn(trimTilTomTekst(kategoriNavn));
		return vinner;
	}

	private String trimTilTomTekst(String tekst) {
		return tekst == null ? "" : tekst.trim();
	}

}
