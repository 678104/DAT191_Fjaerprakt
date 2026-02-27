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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DommerServiceImpl implements DommerService {
	private final Logger logger = LoggerFactory.getLogger(DommerServiceImpl.class);

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
		if (dueId == null) {
			throw new InvalidParameterException("dueId", "cannot be null");
		}
		if (nyBedommelse == null) {
			throw new InvalidParameterException("nyBedommelse", "cannot be null");
		}
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}

		DommerPaamelding dp = dommerPaameldingRepository.finnPaameldingForAktivUtstilling(dommer.getId())
				.orElseThrow(() -> new ResourceNotFoundException("DommerPaamelding", "Ingen aktiv utstilling funnet for dommer"));

		Due due = hentDueMedId(dueId);

		Bedommelse eksisterende = due.getBedommelse();

		if (eksisterende != null) {
			// Oppdater feltene på den eksisterende bedømmelsen
			eksisterende.setPoeng(nyBedommelse.getPoeng());
			eksisterende.setFordeler(nyBedommelse.getFordeler());
			eksisterende.setOnsker(nyBedommelse.getOnsker());
			eksisterende.setFeil(nyBedommelse.getFeil());
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
	public Due hentDueMedId(Long dueId) {
		return dueRepo.findById(dueId).orElseThrow(() -> new DueNotFoundException(dueId));
	}

	@Override
	public List<Due> finnDuerDommerSkalBedomme(Bruker dommer) {
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}

		Optional<DommerPaamelding> dpOpt = dommerPaameldingRepository.finnPaameldingForAktivUtstilling(dommer.getId());
		if (dpOpt.isEmpty()) {
			return List.of();
		}

		DommerPaamelding paamelding = dpOpt.get();

		List<String> raser = RaseStringHjelper.hentUtRaser(paamelding);

		return dueRepo.findByPaamelding_Utstilling_IdAndRaseInIgnoreCaseOrderByBurnummerAsc(
				paamelding.getUtstilling().getId(), raser);
	}

	@Override
	public Due finnDueDommerSkalBedommeMedBurnummer(Bruker dommer, Integer burnummer) {
		if (dommer == null) {
			throw new InvalidParameterException("dommer", "cannot be null");
		}
		if (burnummer == null) {
			throw new InvalidParameterException("burnummer", "cannot be null");
		}

		Optional<DommerPaamelding> dpOpt = dommerPaameldingRepository.finnPaameldingForAktivUtstilling(dommer.getId());
		if (dpOpt.isEmpty()) {
			throw new ResourceNotFoundException("DommerPaamelding", "for dommer with id " + dommer.getId());
		}

		DommerPaamelding paamelding = dpOpt.get();

		List<String> raser = RaseStringHjelper.hentUtRaser(paamelding);

		Due due = dueRepo.finnDuePaameldtUtstillingMedBurnummerOgRiktigRase(paamelding.getUtstilling().getId(), burnummer, raser);
		if (due == null) {
			throw new ResourceNotFoundException("Due", "with burnummer " + burnummer);
		}
		return due;
	}
}
