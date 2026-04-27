package no.hvl.peristeri.feature.paamelding;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.BusinessRuleViolationException;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.DueService;
import no.hvl.peristeri.feature.duekatalog.DueGruppe;
import no.hvl.peristeri.feature.duekatalog.DueKatalogService;
import no.hvl.peristeri.feature.duekatalog.DueRase;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/paamelding")
public class PaameldingController {
	private static final String navLocation = "paamelding";

	private final Logger logger = LoggerFactory.getLogger(PaameldingController.class);

	private final PaameldingService paameldingService;
	private final UtstillingService utstillingService;
	private final DueKatalogService dueKatalogService;

	@GetMapping
	public String paamelding(@AuthenticationPrincipal Bruker bruker,
	                        @RequestParam(required = false) Long utstillingId,
	                        @RequestParam(required = false) Long paameldingId,
	                        Model model,
	                        HttpSession session) {
		if (utstillingId == null) {
			return visUtstillingsvalg(model, bruker, null, null);
		}
		return visDirektePaamelding(model, bruker, session, utstillingId, paameldingId);
	}

	@PostMapping("/paameldingskvittering")
	public String utstillerKvittering(Model model, @AuthenticationPrincipal Bruker bruker, HttpSession session,
	                                  Long utstillingId) {
		if (utstillingId == null) {
			return visUtstillingsvalg(model, bruker, null,
					"Du må velge en utstilling før du kan melde deg på.");
		}
		String view = visDirektePaamelding(model, bruker, session, utstillingId, null);
		if ("paamelding/paameldingkvittering".equals(view)) {
			return "paamelding/paameldingkvittering :: kvittering";
		}
		return view;
	}

	@PostMapping("/duekvittering")
	public String duekvittering(@ModelAttribute DueDTO dueDTO,
	                            @RequestParam(required = false) Long gruppeId,
	                            Model model,
	                            HttpSession session) {
		logger.info("PaameldingDTO: {}", dueDTO);

		boolean ugyldigRase = !dueKatalogService.erRaseGyldigForGruppe(gruppeId, dueDTO.rase());

		if (ugyldigRase) {
			throw new BusinessRuleViolationException("Ugyldig valg for rase, farge eller variant.");
		}

		if (dueDTO.hunnerUng() == 0 && dueDTO.hunnerEldre() == 0 && dueDTO.hannerUng() == 0 &&
		    dueDTO.hannerEldre() == 0) {
			model.addAttribute("errorMessage", "Du må melde på minst én due.");
			return "paamelding/due_tabell :: due-tabell";
		}
		DueDTOList duerDTO = getDueList(session);

		duerDTO.leggTilDueDTO(dueDTO);

		Integer antallDuer = paameldingService.antallDuer(getDueList(session).getListe());
		model.addAttribute("antallDuer", antallDuer);

		return "paamelding/due_tabell :: due-tabell";
	}

	@GetMapping("/redigerDue/{radId}")
	public String redigerDueRad(Model model, @PathVariable Integer radId, HttpSession session) {
		DueDTO dueRad = getDueList(session).findDueById(radId);
		logger.info("Duerad: {}", dueRad);
		model.addAttribute("dueDTO", dueRad);

		return "paamelding/due_tabell :: redigerDueRad";
	}

	@PostMapping("/oppdaterDue/{radId}")
	public String oppdaterDueRad(@ModelAttribute DueDTO due, Model model, @PathVariable Integer radId,
	                             HttpSession session) {
		DueDTO oppdatertDue = due.radId() != null && due.radId().equals(radId)
				? due
				: new DueDTO(radId, due.rase(), due.farge(), due.variant(), due.hannerUng(), due.hannerEldre(),
				due.hunnerUng(), due.hunnerEldre(), due.ikkeEget());
		model.addAttribute("due", oppdatertDue);
		getDueList(session).endreDue(oppdatertDue);
		logger.info("Lagre knapp trykket: {}", oppdatertDue);

		return "paamelding/due_tabell :: oppdatertDueRad";
	}

	@PostMapping("/fjernDue/{radId}")
	public String fjernDueRad(@PathVariable Integer radId, HttpSession session) {
		getDueList(session).fjernDueDTO(radId);
		logger.info("Fjernet due med radId {} fra påmelding", radId);
		return "paamelding/due_tabell :: due-tabell";
	}

	@PostMapping("/submitPaamelding")
	public String submitPaamelding(@AuthenticationPrincipal Bruker bruker, Model model, HttpSession session) {
		Integer antallDuer = paameldingService.antallDuer(getDueList(session).getListe());
		model.addAttribute("antallDuer", antallDuer);

		BigDecimal totalPris = paameldingService.beregnTotalPris(antallDuer,
				((Utstilling) session.getAttribute("utstilling")).getDuePris());
		session.setAttribute("totalPris", totalPris);

		Long redigerPaameldingId = (Long) session.getAttribute("redigerPaameldingId");
		if (redigerPaameldingId != null) {
			paameldingService.oppdaterPaamelding(redigerPaameldingId, bruker.getId(), getDueList(session),
					(BigDecimal) session.getAttribute("totalPris"));
			session.removeAttribute("redigerPaameldingId");
		} else {
			paameldingService.leggTilPaamelding(bruker.getId(), ((Utstilling) session.getAttribute("utstilling")).getId(),
					getDueList(session), (BigDecimal) session.getAttribute("totalPris"));
		}

		logger.info("Utstiller: {}, Utstilling: {}, DueDTOList: {}", bruker, session.getAttribute("utstilling"),
				getDueList(session));
		model.addAttribute("utstiller", bruker);
		return "paamelding/paameldt :: paameldt";
	}

	@GetMapping("/{utstillingId}")
	public String meldPaaMedValgtUtstillingFraFoer(@PathVariable Long utstillingId) {
		return "redirect:/paamelding?utstillingId=" + utstillingId;
	}

	private String visDirektePaamelding(Model model,
	                                   Bruker bruker,
	                                   HttpSession session,
	                                   Long utstillingId,
	                                   Long paameldingId) {
		model.addAttribute("utstiller", bruker);

		Utstilling utstilling;
		if (paameldingId != null) {
			Paamelding paamelding = paameldingService.hentPaamelding(paameldingId);
			if (!erEgenPaamelding(bruker, paamelding) || !utstillingId.equals(paamelding.getUtstilling().getId())) {
				return visUtstillingsvalg(model, bruker, utstillingId, "Du har ikke tilgang til å endre denne påmeldingen.");
			}
			utstilling = paamelding.getUtstilling();
			if (erPaameldingsfristUtlopt(utstilling)) {
				return visUtstillingsvalg(model, bruker, utstillingId,
						"Påmeldingsfristen er utløpt. Du kan ikke endre påmeldingen.");
			}
			session.setAttribute("redigerPaameldingId", paameldingId);
			session.setAttribute("dueDTOListe", byggDueDTOListFraPaamelding(paamelding));
		} else {
			List<Utstilling> utstillinger = utstillingService.finnUtstillingerMedMulighetForPaamelding();
			utstilling = finnGyldigUtstilling(utstillinger, utstillingId);
			if (utstilling == null) {
				return visUtstillingsvalg(model, bruker, utstillingId,
						"Fant ikke valgt utstilling, eller påmelding er stengt.");
			}
		}

		if (paameldingId == null && paameldingService.sjekkOmBrukerAlleredeErPaameldt(bruker, utstilling)) {
			return visUtstillingsvalg(model, bruker, utstillingId,
					"Du er allerede påmeldt denne utstillingen.");
		}

		if (paameldingId == null) {
			session.removeAttribute("redigerPaameldingId");
			session.setAttribute("dueDTOListe", new DueDTOList());
		}

		session.setAttribute("utstilling", utstilling);

		logger.info("Påmelding startet for utstilling: {} ({})", utstilling.getTittel(), utstillingId);
		model.addAttribute("radId", finnNesteRadId(getDueList(session)));
		leggTilDueKatalogModel(model, null);
		return "paamelding/paameldingkvittering";
	}

	private String visUtstillingsvalg(Model model,
	                                 Bruker bruker,
	                                 Long forhandsvalgtUtstillingId,
	                                 String feilmelding) {
		model.addAttribute("utstiller", bruker);
		model.addAttribute("forhandsvalgtUtstillingId", forhandsvalgtUtstillingId);
		if (feilmelding != null && !feilmelding.isBlank()) {
			model.addAttribute("ingenUtstilling", feilmelding);
		}
		List<Utstilling> utstillinger = utstillingService.finnUtstillingerMedMulighetForPaamelding();
		model.addAttribute("utstillinger", utstillinger);
		return "paamelding/paamelding";
	}

	@GetMapping("/raser")
	public String hentRaserForGruppe(@RequestParam(required = false) Long gruppeId, Model model) {
		Long valgtGruppeId = finnValgtEllerForsteGruppeId(gruppeId);
		List<DueRase> raser = valgtGruppeId == null ? List.of() : dueKatalogService.finnRaserForGruppe(valgtGruppeId);
		model.addAttribute("dueRaser", raser);
		return "paamelding/paameldingkvittering :: ny-due-rase-select";
}

	private Utstilling finnGyldigUtstilling(List<Utstilling> tilgjengeligeUtstillinger, Long utstillingId) {
		if (utstillingId == null || tilgjengeligeUtstillinger == null) {
			return null;
		}
		return tilgjengeligeUtstillinger.stream()
				.filter(u -> u.getId() != null && u.getId().equals(utstillingId))
				.findFirst()
				.orElse(null);
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

	public DueDTOList getDueList(HttpSession session) {
		DueDTOList duerDTO = (DueDTOList) session.getAttribute("dueDTOListe");
		if (duerDTO == null) {
			duerDTO = new DueDTOList();
			session.setAttribute("dueDTOListe", duerDTO);
		}
		return duerDTO;
	}

	private void leggTilDueKatalogModel(Model model, Long gruppeId) {
		Long valgtGruppeId = finnValgtEllerForsteGruppeId(gruppeId);
		List<DueGruppe> grupper = dueKatalogService.finnAlleGrupper();
		List<DueRase> raser = valgtGruppeId == null ? List.of() : dueKatalogService.finnRaserForGruppe(valgtGruppeId);
		model.addAttribute("dueGrupper", grupper);
		model.addAttribute("valgtGruppeId", valgtGruppeId);
		model.addAttribute("dueRaser", raser);
		model.addAttribute("dueFarger", dueKatalogService.finnAlleFarger());
		model.addAttribute("dueVarianter", dueKatalogService.finnAlleVarianter());
	}

	private Long finnValgtEllerForsteGruppeId(Long gruppeId) {
		if (gruppeId != null) {
			return gruppeId;
		}
		return dueKatalogService.finnAlleGrupper().stream()
				.findFirst()
				.map(DueGruppe::getId)
				.orElse(null);
	}

	private boolean erTom(String verdi) {
		return verdi == null || verdi.isBlank();
	}

	private boolean erEgenPaamelding(Bruker bruker, Paamelding paamelding) {
		return bruker != null
				&& paamelding != null
				&& paamelding.getUtstiller() != null
				&& bruker.getId().equals(paamelding.getUtstiller().getId());
	}

	private boolean erPaameldingsfristUtlopt(Utstilling utstilling) {
		if (utstilling == null) {
			return true;
		}
		LocalDate frist = utstilling.getPaameldingsFrist();
		return frist != null && LocalDate.now().isAfter(frist);
	}

	private int finnNesteRadId(DueDTOList dueDTOList) {
		int max = 0;
		for (DueDTO dueDTO : dueDTOList.getListe()) {
			if (dueDTO.radId() != null && dueDTO.radId() > max) {
				max = dueDTO.radId();
			}
		}
		return max + 1;
	}

	private DueDTOList byggDueDTOListFraPaamelding(Paamelding paamelding) {
		DueDTOList resultat = new DueDTOList();
		Map<String, DueAggregat> aggregatMap = new LinkedHashMap<>();
		for (var due : paamelding.getDuer()) {
			String noekkel = lagNoekkel(due.getRase(), due.getFarge(), due.getVariant(), due.getIkkeEget());
			DueAggregat aggregat = aggregatMap.computeIfAbsent(noekkel,
					ignored -> new DueAggregat(due.getRase(), due.getFarge(), due.getVariant(), due.getIkkeEget()));
			aggregat.inkrementer(due.getKjonn(), due.getAlder());
		}

		int radId = 1;
		for (DueAggregat aggregat : aggregatMap.values()) {
			resultat.leggTilDueDTO(new DueDTO(
					radId++,
					aggregat.rase,
					aggregat.farge,
					aggregat.variant,
					aggregat.hannerUng,
					aggregat.hannerEldre,
					aggregat.hunnerUng,
					aggregat.hunnerEldre,
					aggregat.ikkeEget
			));
		}
		return resultat;
	}

	private String lagNoekkel(String rase, String farge, String variant, Boolean ikkeEget) {
		return String.join("|",
				rase == null ? "" : rase,
				farge == null ? "" : farge,
				variant == null ? "" : variant,
				String.valueOf(Boolean.TRUE.equals(ikkeEget)));
	}

	private static class DueAggregat {
		private final String rase;
		private final String farge;
		private final String variant;
		private final Boolean ikkeEget;
		private int hannerUng;
		private int hannerEldre;
		private int hunnerUng;
		private int hunnerEldre;

		private DueAggregat(String rase, String farge, String variant, Boolean ikkeEget) {
			this.rase = rase;
			this.farge = farge;
			this.variant = variant;
			this.ikkeEget = Boolean.TRUE.equals(ikkeEget);
		}

		private void inkrementer(Boolean hann, Boolean eldre) {
			if (Boolean.TRUE.equals(hann)) {
				if (Boolean.TRUE.equals(eldre)) {
					hannerEldre++;
				} else {
					hannerUng++;
				}
			} else {
				if (Boolean.TRUE.equals(eldre)) {
					hunnerEldre++;
				} else {
					hunnerUng++;
				}
			}
		}
	}
}
