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

import java.util.List;

@Service
@RequiredArgsConstructor
public class DommerServiceImpl implements DommerService {

	private final DueRepository              dueRepo;
	private final BedommelseRepository       bedommelseRepo;
	private final UtstillingRepository       utstillingRepo;
	private final DommerPaameldingRepository dommerPaameldingRepository;
	private final BrukerService              brukerService;

	@Override
	public List<Due> hentAlleDuer() {
		List<Due> liste = dueRepo.findAll();
		System.out.println("Fant " + liste.size() + " duer i databasen.");
		return liste;
	}

	@Override
	public void lagreBedommelse(Long dueId, Bedommelse nyBedommelse, Bruker dommer) {
		lagreBedommelse(dueId, nyBedommelse, dommer, null);
	}

	@Override
	public void lagreBedommelse(Long dueId, Bedommelse nyBedommelse, Bruker dommer, Long utstillingId) {
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

			bedommelseRepo.save(eksisterende);
		} else {
			// Ny bedømmelse
			nyBedommelse.setDue(due);
			due.setBedommelse(nyBedommelse);

			nyBedommelse.setBedomtAv(dp);

			bedommelseRepo.save(nyBedommelse);
			dueRepo.save(due);
		}
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
		Utstilling utstilling = utstillingRepo.findById(utstillingId)
		                                      .orElseThrow(() -> new ResourceNotFoundException("Utstilling", utstillingId));
		DommerPaamelding dommerPaamelding = new DommerPaamelding(utstilling, saved);
		return dommerPaameldingRepository.save(dommerPaamelding);
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
}
