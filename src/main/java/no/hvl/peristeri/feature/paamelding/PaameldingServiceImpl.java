package no.hvl.peristeri.feature.paamelding;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.due.DueRepository;
import no.hvl.peristeri.feature.due.DueService;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class PaameldingServiceImpl implements PaameldingService {
	private final Logger logger = LoggerFactory.getLogger(PaameldingServiceImpl.class);

	private final PaameldingRepository paameldingRepository;
	private final BrukerService        brukerService;
	private final UtstillingService    utstillingService;
	private final DueService           dueService;
	private final DueRepository        dueRepository;


	/**
	 * Denne metoden forventer at påmeldingen er rikitg koblet opp før den brukes
	 *
	 * @param paamelding Påmeldingen som skal legges til
	 *
	 * @return Den lagrede påmeldingen
	 */
	@Override
	public Paamelding manueltLeggTilPaamelding(Paamelding paamelding) {
		return paameldingRepository.save(paamelding);
	}


	@Transactional
	@Override
	public Paamelding leggTilPaamelding(Long utstillerId, Long utstillingId, DueDTOList duerDTO,
	                                    BigDecimal paameldingsAvgift) {
		if (utstillerId == null) {
			throw new InvalidParameterException("utstillerId", "cannot be null");
		}
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}
		if (duerDTO == null) {
			throw new InvalidParameterException("duerDTO", "cannot be null");
		}

		Bruker     utstiller  = brukerService.hentBrukerMedId(utstillerId);
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);

		Paamelding paamelding = new Paamelding(utstiller, utstilling);
		paamelding.setPaameldingsAvgift(paameldingsAvgift);
		Paamelding savedPaamelding = paameldingRepository.save(paamelding);


		// Setter due sin utstiller til den som er lagret i paamelding
		for (DueDTO dueDTO : duerDTO.getListe()) {
			List<Due> duer = konverterDueDTOtilDue(dueDTO);

			for (Due due : duer) {
				due.setPaamelding(savedPaamelding);
				dueService.leggTilDue(due);
			}
		}

		logger.info("Paamelding opprettet: {}", savedPaamelding);
		return savedPaamelding;
	}

	@Transactional
	@Override
	public Paamelding oppdaterPaamelding(Long paameldingId, Long utstillerId, DueDTOList duerDTO,
	                                     BigDecimal paameldingsAvgift) {
		if (paameldingId == null) {
			throw new InvalidParameterException("paameldingId", "cannot be null");
		}
		if (utstillerId == null) {
			throw new InvalidParameterException("utstillerId", "cannot be null");
		}
		if (duerDTO == null) {
			throw new InvalidParameterException("duerDTO", "cannot be null");
		}

		Paamelding paamelding = hentPaamelding(paameldingId);
		if (!utstillerId.equals(paamelding.getUtstiller().getId())) {
			throw new InvalidParameterException("paameldingId", "Du har ikke tilgang til å endre denne påmeldingen.");
		}

		Map<String, List<Due>> eksisterendeDuerMedRingnummer = new java.util.HashMap<>();
		for (Due d : paamelding.getDuer()) {
			eksisterendeDuerMedRingnummer
					.computeIfAbsent(lagDueNoekkel(d), ignored -> new ArrayList<>())
					.add(d);
		}

		dueRepository.deleteAllByPaamelding_Id(paameldingId);

		for (DueDTO dueDTO : duerDTO.getListe()) {
			List<Due> nyeDuer = konverterDueDTOtilDue(dueDTO);
			for (Due ny : nyeDuer) {
				ny.setPaamelding(paamelding);
				bevarRingnummerHvisMulig(ny, eksisterendeDuerMedRingnummer);
				dueService.leggTilDue(ny);
			}
		}

		paamelding.setPaameldingsAvgift(paameldingsAvgift);
		// Paamelding er allerede managed i transaksjonen. Unngå save/merge her,
		// ellers kan Hibernate forsøke å merge slettede Due-instanser.
		logger.info("Paamelding oppdatert: {}", paamelding);
		return paamelding;
	}

	/**
	 * Henter påmeldinger til en bruker, og deler dem opp i kommende og tidligere påmeldinger
	 *
	 * @param bruker Brukeren som skal hentes påmeldinger for
	 *
	 * @return En map med kommende og tidligere påmeldinger
	 */
	@Override
	public Map<String, List<Paamelding>> hentBrukerSinePaameldinger(Bruker bruker) {
		if (bruker == null) {
			throw new InvalidParameterException("bruker", "cannot be null");
		}
		List<Paamelding>              liste        = paameldingRepository.findByUtstiller(bruker);
		Map<String, List<Paamelding>> paameldinger = new java.util.HashMap<>();
		List<Paamelding>              kommende     = new ArrayList<>();
		List<Paamelding>              tidligere    = new ArrayList<>();
		LocalDate                     now          = LocalDate.now();
		for (Paamelding p : liste) {
			if (p.getUtstilling().getDatoRange().getEndDate().isBefore(now)) {
				tidligere.add(p);
			} else {
				kommende.add(p);
			}
		}
		paameldinger.put("kommende", kommende);
		paameldinger.put("tidligere", tidligere);
		return paameldinger;
	}

	@Override
	public Paamelding hentPaamelding(Long id) {
		if (id == null) {
			throw new InvalidParameterException("id", "cannot be null");
		}
		return paameldingRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Paamelding", id));
	}

	@Override
	public List<Due> konverterDueDTOtilDue(DueDTO dueDTO) {
		List<Due> duer = new ArrayList<>();
		for (int i = 0; i < dueDTO.hannerUng(); i++) {
			duer.add(new Due(dueDTO.rase(), dueDTO.farge(), dueDTO.variant(), true, false, dueDTO.ikkeEget()));
		}
		for (int i = 0; i < dueDTO.hannerEldre(); i++) {
			duer.add(new Due(dueDTO.rase(), dueDTO.farge(), dueDTO.variant(), true, true, dueDTO.ikkeEget()));
		}
		for (int i = 0; i < dueDTO.hunnerUng(); i++) {
			duer.add(new Due(dueDTO.rase(), dueDTO.farge(), dueDTO.variant(), false, false, dueDTO.ikkeEget()));
		}
		for (int i = 0; i < dueDTO.hunnerEldre(); i++) {
			duer.add(new Due(dueDTO.rase(), dueDTO.farge(), dueDTO.variant(), false, true, dueDTO.ikkeEget()));
		}

		return duer;
	}

	@Override
	public Integer antallDuer(List<DueDTO> liste) {
		Integer antall = 0;
		for (DueDTO dueDTO : liste) {
			antall += dueDTO.hannerUng();
			antall += dueDTO.hannerEldre();
			antall += dueDTO.hunnerUng();
			antall += dueDTO.hunnerEldre();
		}

		return antall;
	}

	@Override
	public BigDecimal beregnTotalPris(Integer antallDuer, BigDecimal utstilling) {
		if (antallDuer == null) {
			throw new InvalidParameterException("antallDuer", "cannot be null");
		}
		if (utstilling == null) {
			throw new InvalidParameterException("utstilling", "cannot be null");
		}
		BigDecimal total = BigDecimal.valueOf(antallDuer).multiply(utstilling);
		logger.info("Total pris: {}", total);
		return total;
	}

	@Override
	public Paamelding erBrukerPaameldtUtstilling(Bruker bruker, Utstilling utstilling) {
		return paameldingRepository.findByUtstillerAndUtstilling(bruker, utstilling).orElse(null);
	}

	@Override
	public boolean sjekkOmBrukerAlleredeErPaameldt(Bruker bruker, Utstilling utstilling) {
		if (bruker == null) {
			throw new InvalidParameterException("bruker", "cannot be null");
		}
		if (utstilling == null) {
			throw new InvalidParameterException("utstilling", "cannot be null");
		}
		return paameldingRepository.findByUtstillerAndUtstilling(bruker, utstilling)
				.isPresent();
	}

	@Override
	public List<Paamelding> hentPaameldingerForUtstilling(Long utstillingId) {
		if (utstillingId == null) {
			throw new InvalidParameterException("utstillingId", "cannot be null");
		}
		return paameldingRepository.findByUtstillingIdSortert(utstillingId);
	}

	private String lagDueNoekkel(Due due) {
		return String.join("|",
				safe(due.getRase()),
				safe(due.getFarge()),
				safe(due.getVariant()),
				String.valueOf(Boolean.TRUE.equals(due.getIkkeEget())),
				String.valueOf(Boolean.TRUE.equals(due.getKjonn())),
				String.valueOf(Boolean.TRUE.equals(due.getAlder())));
	}

	private String safe(String verdi) {
		return verdi == null ? "" : verdi;
	}

	private void bevarRingnummerHvisMulig(Due nyDue, Map<String, List<Due>> eksisterendeDuerMedRingnummer) {
		List<Due> kandidater = eksisterendeDuerMedRingnummer.get(lagDueNoekkel(nyDue));
		if (kandidater == null || kandidater.isEmpty()) {
			return;
		}
		Due kandidat = kandidater.remove(0);
		nyDue.setLopenummer(kandidat.getLopenummer());
		nyDue.setAarstall(kandidat.getAarstall());
	}
}
