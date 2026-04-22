package no.hvl.peristeri.feature.admin;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxReswap;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import no.hvl.peristeri.feature.bruker.Rolle;
import no.hvl.peristeri.feature.dommer.DommerPaamelding;
import no.hvl.peristeri.feature.dommer.DommerService;
import no.hvl.peristeri.feature.dommer.BedommingsKategori;
import no.hvl.peristeri.feature.dommer.StandardKommentarService;
import no.hvl.peristeri.feature.dommer.StandardKommentarType;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.due.DueService;
import no.hvl.peristeri.feature.duekatalog.DueKatalogService;
import no.hvl.peristeri.feature.utstilling.KatalogPdfService;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import no.hvl.peristeri.util.RaseStringHjelper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminController {
	private static final String navLocation = "admin";


	private final BrukerService     brukerService;
	private final UtstillingService utstillingService;
	private final DommerService     dommerService;
	private final DueService        dueService;
	private final StandardKommentarService standardKommentarService;
	private final DueKatalogService dueKatalogService;
	private final KatalogPdfService katalogPdfService;

	@GetMapping
	public String getAdmin(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
		model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
		return "admin/admin";
	}

	@GetMapping("/panel")
	public String getAdminPanel() {
		Utstilling aktivUtstilling = utstillingService.finnAktivUtstilling();
		if (aktivUtstilling != null) {
			return "redirect:/admin/" + aktivUtstilling.getId();
		}
		return "redirect:/admin";
	}

	@GetMapping("/dommere")
	public String getDommereAdmin(Model model) {
		settDommerAdminModel(model);
		model.addAttribute("fragment", "dommerAdministrasjon");
		return "admin/admin";
	}

	@GetMapping("/bedomming-oppsett")
	public String getBedommingOppsett(Model model) {
		model.addAttribute("hovedkategorier", BedommingsKategori.values());
		model.addAttribute("fordelerKommentarer", standardKommentarService.hentKommentarerPerKategori(StandardKommentarType.STANDARD));
		model.addAttribute("onskerKommentarer", standardKommentarService.hentKommentarerPerKategori(StandardKommentarType.ONSKER));
		model.addAttribute("feilKommentarer", standardKommentarService.hentKommentarerPerKategori(StandardKommentarType.FEIL));
		return "admin/admin_standardkommentarer";
	}

	@PostMapping("/bedomming-oppsett/legg-til")
	public String postLeggTilStandardKommentar(@RequestParam BedommingsKategori kategori,
	                                          @RequestParam StandardKommentarType type,
	                                          @RequestParam String tekst) {
		standardKommentarService.leggTilKommentar(kategori, type, tekst);
		return "redirect:/admin/bedomming-oppsett";
	}

	@PostMapping("/bedomming-oppsett/{id}/slett")
	public String postSlettStandardKommentar(@PathVariable Long id) {
		standardKommentarService.slettKommentar(id);
		return "redirect:/admin/bedomming-oppsett";
	}

	@HxRequest
	@GetMapping("/{id}")
	public String getAdminUtstillingHtmx(@PathVariable Long id, Model model, HttpSession session,
	                                     RedirectAttributes redirectAttributes) {
		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(id));
		return "admin/admin_fragments :: adminUtstilling";
	}


	@GetMapping("/{id}")
	public String getAdminUtstilling(@PathVariable Long id, Model model, HttpSession session,
	                                 RedirectAttributes redirectAttributes) {
		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(id));
		model.addAttribute("fragment", "adminUtstilling");
		return "admin/admin";
	}

	@HxRequest
	@GetMapping("/{id}/tildel-dommer-rolle")
	public String getTildelDommerRolleHtmx(@PathVariable Long id, Model model, HttpSession session,
	                                       RedirectAttributes redirectAttributes) {
		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(id));
		model.addAttribute("brukere", List.of());
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(id));
		model.addAttribute("dommere", dommerService.hentDommere());
		return "admin/admin_fragments :: tildelDommerRolle";
	}

	@GetMapping("/brukere")
	public String getBrukereAutocompleteHtmx(@RequestParam(name = "søk", required = false) String soek,
	                                        @RequestParam(name = "sok", required = false) String sok,
	                                        Model model) {
		String filter = hentSoketekst(soek, sok);
		model.addAttribute("brukere", brukerService.finnBrukereForDommerAutocomplete(filter, 10));
		model.addAttribute("sok", filter);
		return "admin/admin_fragments :: brukerForslag";
	}

	@HxRequest
	@GetMapping("/dommere/brukere")
	public String getDommereBrukereHtmx(@RequestParam(required = false) String sok, Model model) {
		String filter = hentSoketekst(null, sok);
		model.addAttribute("brukere", filter.isBlank() ? List.of() : brukerService.finnBrukere(filter).stream().limit(10).toList());
		model.addAttribute("sok", filter);
		return "admin/admin_fragments :: brukerForslag";
	}

	@HxRequest
	@GetMapping("/admins/brukere")
	public String getAdminsBrukereHtmx(@RequestParam(required = false) String sok, Model model) {
		String filter = hentSoketekst(null, sok);
		model.addAttribute("brukere", filter.isBlank() ? List.of() : brukerService.finnBrukereForDommerAutocomplete(filter, 10));
		model.addAttribute("sok", filter);
		return "admin/admin_fragments :: brukerForslagAdmin";
	}

	@HxRequest
	@GetMapping("/{id}/tildel-dommer-rolle/brukere")
	public String getTildelDommerRolleBrukereHtmx(@PathVariable Long id, @RequestParam(required = false) String sok,
	                                             Model model) {
		String filter = hentSoketekst(null, sok);
		model.addAttribute("brukere", filter.isBlank() ? List.of() : brukerService.finnBrukere(filter).stream().limit(10).toList());
		model.addAttribute("sok", filter);
		return "admin/admin_fragments :: brukerForslag";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/tildel-dommer-rolle")
	public String postTildelDommerRolle(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                    RedirectAttributes redirectAttributes, @RequestParam(required = false) Long brukerId,
	                                    HtmxResponse htmxResponse) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("brukere", List.of());
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(utstillingId));
		model.addAttribute("dommere", dommerService.hentDommere());

		if (brukerId == null) {
			model.addAttribute("brukerFeil", "Velg bruker å tildele rolle");
			htmxResponse.setReswap(HtmxReswap.outerHtml());
			htmxResponse.setRetarget("main-content");
			return "admin/admin_fragments :: tildelDommerRolle";
		}

		dommerService.tildelDommerTilUtstillinger(brukerId, List.of(utstillingId));
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(utstillingId));
		model.addAttribute("dommere", dommerService.hentDommere());
		return "admin/admin_fragments :: tildelDommerRolle";
	}

	@HxRequest
	@PostMapping("/dommere/rolle")
	public String postLeggTilDommerRolleFraSkjema(@RequestParam(required = false) Long brukerId, Model model, HtmxResponse htmxResponse) {
		if (brukerId == null) {
			model.addAttribute("alleDommereFeil", "Velg bruker å tildele rolle");
		} else {
			try {
				brukerService.hentBrukerMedId(brukerId);
				if (brukerService.harRolle(brukerId, Rolle.DOMMER)) {
					model.addAttribute("alleDommereFeil", "Brukeren har allerede dommerrolle.");
				} else {
					dommerService.tildelDommerRolle(brukerId);
				}
			} catch (ResourceNotFoundException ex) {
				model.addAttribute("alleDommereFeil", "Fant ikke brukeren du prøvde å gi dommerrolle.");
			}
		}

		settAlleDommereListeModel(model);
		htmxResponse.setReswap(HtmxReswap.outerHtml());
		htmxResponse.setRetarget("#main-content");
		settDommerAdminModel(model);
		return "admin/admin_fragments :: dommerAdministrasjon";
	}

	@HxRequest
	@PostMapping("/dommere/{brukerId}/rolle")
	public String postLeggTilDommerRolle(@PathVariable Long brukerId, Model model, HtmxResponse htmxResponse) {
		try {
			brukerService.hentBrukerMedId(brukerId);
			if (brukerService.harRolle(brukerId, Rolle.DOMMER)) {
				model.addAttribute("alleDommereFeil", "Brukeren har allerede dommerrolle.");
			} else {
				dommerService.tildelDommerRolle(brukerId);
			}
		} catch (ResourceNotFoundException ex) {
			model.addAttribute("alleDommereFeil", "Fant ikke brukeren du prøvde å gi dommerrolle.");
		}

		settAlleDommereListeModel(model);
		htmxResponse.setReswap(HtmxReswap.outerHtml());
		htmxResponse.setRetarget("#main-content");
		settDommerAdminModel(model);
		return "admin/admin_fragments :: dommerAdministrasjon";
	}

	@HxRequest
	@DeleteMapping("/dommere/{brukerId}/rolle")
	public String deleteDommerRolle(@PathVariable Long brukerId, Model model, HtmxResponse htmxResponse) {
		try {
			brukerService.hentBrukerMedId(brukerId);
			List<Utstilling> aktiveUtstillinger = dommerService.hentUtstillingerForDommer(brukerId).stream()
					.filter(utstilling -> utstilling.getAktiv() != null && utstilling.getAktiv())
					.toList();

			if (!aktiveUtstillinger.isEmpty()) {
				model.addAttribute("alleDommereFeil", "Brukeren er tilknyttet aktive utstillinger og kan ikke få fjernet dommerrollen.");
			} else {
				dommerService.fjernDommerRolle(brukerId);
			}
		} catch (ResourceNotFoundException ex) {
			model.addAttribute("alleDommereFeil", "Fant ikke brukeren du prøvde å fjerne dommerrolle fra.");
		}

		settAlleDommereListeModel(model);
		htmxResponse.setReswap(HtmxReswap.outerHtml());
		htmxResponse.setRetarget("#main-content");
		settDommerAdminModel(model);
		return "admin/admin_fragments :: dommerAdministrasjon";
	}

	@HxRequest
	@PostMapping("/dommere/tildel-utstillinger")
	public String postTildelUtstillingerTilDommer(@RequestParam(required = false) Long brukerId,
	                                            @RequestParam(required = false) List<Long> utstillingIder,
	                                            Model model,
	                                            HtmxResponse htmxResponse) {
		if (brukerId == null) {
			model.addAttribute("dommerTildelingFeil", "Velg dommer å tildele utstillinger");
			settDommerTildelingModel(model);
			htmxResponse.setReswap(HtmxReswap.outerHtml());
			htmxResponse.setRetarget("#dommerTildelingListe");
			return "admin/admin_fragments :: dommerTildelingListe";
		}

		dommerService.tildelDommerTilUtstillinger(brukerId, utstillingIder == null ? List.of() : utstillingIder);
		settDommerTildelingModel(model);
		htmxResponse.setReswap(HtmxReswap.outerHtml());
		htmxResponse.setRetarget("#dommerTildelingListe");
		return "admin/admin_fragments :: dommerTildelingListe";
	}

	@HxRequest
	@PostMapping("/dommere/tildelinger/{dommerPaameldingId}/fjern")
	public String postFjernDommerFraUtstilling(@PathVariable Long dommerPaameldingId,
	                                           Model model,
	                                           HtmxResponse htmxResponse) {
		DommerPaamelding tildeling = hentAlleDommerTildelinger().stream()
				.filter(dp -> dp.getId().equals(dommerPaameldingId))
				.findFirst()
				.orElse(null);

		if (tildeling == null) {
			model.addAttribute("dommerTildelingFeil", "Fant ikke dommertildelingen du prøvde å fjerne.");
		} else {
			try {
				dommerService.fjernDommerPaamelding(dommerPaameldingId);
			} catch (ResourceNotFoundException ex) {
				model.addAttribute("dommerTildelingFeil", "Dommertildelingen finnes ikke lenger.");
			}
		}

		settDommerTildelingModel(model);
		htmxResponse.setReswap(HtmxReswap.outerHtml());
		htmxResponse.setRetarget("#dommerTildelingListe");
		return "admin/admin_fragments :: dommerTildelingListe";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/fjern-dommer/{dommerPaameldingId}")
	public String postFjernDommer(@PathVariable Long utstillingId, @PathVariable Long dommerPaameldingId, 
	                               Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		List<DommerPaamelding> dommerePaameldtUtstilling = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId);
		boolean erKnyttetTilUtstilling = dommerePaameldtUtstilling.stream()
				.anyMatch(dp -> dp.getId().equals(dommerPaameldingId));

		if (!erKnyttetTilUtstilling) {
			model.addAttribute("dommerListeFeil", "Dommeren er ikke tilknyttet denne utstillingen.");
		} else {
			try {
				dommerService.fjernDommerPaamelding(dommerPaameldingId);
			} catch (ResourceNotFoundException ex) {
				model.addAttribute("dommerListeFeil", "Dommeren finnes ikke lenger.");
			}
		}

		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(utstillingId));
		return "admin/admin_fragments :: dommerListe";
	}

	private void settDommerAdminModel(Model model) {
		settAlleDommereListeModel(model);
		settAlleAdminListeModel(model);
		model.addAttribute("alleBrukere", brukerService.getBrukere());
		model.addAttribute("utstillinger", utstillingService.finnIkkeTidligereUtstillinger());
		settDommerTildelingModel(model);
		model.addAttribute("brukere", List.of());
		model.addAttribute("fragment", "dommerAdministrasjon");
	}

	private void settAlleAdminListeModel(Model model) {
		model.addAttribute("administratorer", brukerService.hentBrukereMedRolle(Rolle.ADMIN));
	}

	@HxRequest
	@PostMapping("/admins/rolle")
	public String postLeggTilAdminRolle(@RequestParam(required = false) Long brukerId,
	                                   Model model,
	                                   HtmxResponse htmxResponse) {
		if (brukerId == null) {
			model.addAttribute("alleAdminFeil", "Velg bruker å tildele adminrolle");
		} else {
			try {
				brukerService.hentBrukerMedId(brukerId);
				if (brukerService.harRolle(brukerId, Rolle.ADMIN)) {
					model.addAttribute("alleAdminFeil", "Brukeren har allerede adminrolle.");
				} else {
					brukerService.leggTilRolle(brukerId, Rolle.ADMIN);
				}
			} catch (ResourceNotFoundException ex) {
				model.addAttribute("alleAdminFeil", "Fant ikke brukeren du prøvde å gi adminrolle.");
			}
		}

		htmxResponse.setReswap(HtmxReswap.outerHtml());
		htmxResponse.setRetarget("#main-content");
		settDommerAdminModel(model);
		return "admin/admin_fragments :: dommerAdministrasjon";
	}

	@HxRequest
	@DeleteMapping("/admins/{brukerId}/rolle")
	public String deleteAdminRolle(@PathVariable Long brukerId,
	                              @AuthenticationPrincipal Bruker innloggetBruker,
	                              Model model,
	                              HtmxResponse htmxResponse) {
		try {
			Bruker bruker = brukerService.hentBrukerMedId(brukerId);
			if (!brukerService.harRolle(brukerId, Rolle.ADMIN)) {
				model.addAttribute("alleAdminFeil", "Brukeren har ikke adminrolle.");
			} else if (innloggetBruker != null && innloggetBruker.getId() != null && innloggetBruker.getId().equals(brukerId)) {
				model.addAttribute("alleAdminFeil", "Du kan ikke fjerne din egen adminrolle.");
			} else if (brukerService.hentBrukereMedRolle(Rolle.ADMIN).size() <= 1) {
				model.addAttribute("alleAdminFeil", "Kan ikke fjerne siste administrator.");
			} else {
				brukerService.fjernRolle(bruker.getId(), Rolle.ADMIN);
			}
		} catch (ResourceNotFoundException ex) {
			model.addAttribute("alleAdminFeil", "Fant ikke brukeren du prøvde å fjerne adminrolle fra.");
		}

		htmxResponse.setReswap(HtmxReswap.outerHtml());
		htmxResponse.setRetarget("#main-content");
		settDommerAdminModel(model);
		return "admin/admin_fragments :: dommerAdministrasjon";
	}

	private void settAlleDommereListeModel(Model model) {
		List<Bruker> dommere = dommerService.hentDommere();
		model.addAttribute("dommere", dommere);
		Map<Long, List<Utstilling>> utstillingerPerDommer = new LinkedHashMap<>();
		for (Bruker dommer : dommere) {
			utstillingerPerDommer.put(dommer.getId(), dommerService.hentUtstillingerForDommer(dommer.getId()));
		}
		model.addAttribute("utstillingerPerDommer", utstillingerPerDommer);
	}

	private void settDommerTildelingModel(Model model) {
		model.addAttribute("dommerTildelinger", hentAlleDommerTildelinger());
	}

	private List<DommerPaamelding> hentAlleDommerTildelinger() {
		return dommerService.hentDommere().stream()
				.flatMap(dommer -> dommerService.finnDommerPaameldinger(dommer).stream())
				.toList();
	}

	private String hentSoketekst(String soek, String sok) {
		if (soek != null && !soek.isBlank()) {
			return soek.trim();
		}
		if (sok != null && !sok.isBlank()) {
			return sok.trim();
		}
		return "";
	}

	@HxRequest
	@GetMapping("/{utstillingId}/dommerFordeling")
	public String getDommerFordelingHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                     RedirectAttributes redirectAttributes) {
		settDommerFordelingModel(model, utstillingId);
		return "admin/admin_fragments :: dommerFordeling";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/fordel-dommer")
	public String postFordelDommerHtmx(@PathVariable Long utstillingId,
	                                   Model model,
	                                   HttpSession session,
	                                   RedirectAttributes redirectAttributes,
	                                   @RequestParam Long dommerPaameldingId,
	                                   @RequestParam(value = "raser", required = false) List<String> raser) {
		List<String> valgteRaser = raser == null ? List.of() : raser.stream()
				.filter(rase -> rase != null && !rase.isBlank())
				.distinct()
				.toList();

		if (!valgteRaser.isEmpty()) {
			DommerPaamelding valgtDommer = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId).stream()
					.filter(dp -> dp.getId().equals(dommerPaameldingId))
					.findFirst()
					.orElseThrow();

			List<String> eksisterende = new ArrayList<>(RaseStringHjelper.hentUtRaser(valgtDommer));
			eksisterende.addAll(valgteRaser);
			List<String> oppdatert = eksisterende.stream().filter(r -> r != null && !r.isBlank()).distinct().toList();
			dommerService.fordelRaserTilDommer(dommerPaameldingId, oppdatert);
			dommerService.tilordneDuerTilDommerEtterRaser(dommerPaameldingId, utstillingId, valgteRaser);
		}

		settDommerFordelingModel(model, utstillingId);
		return "admin/admin_fragments :: dommerFordeling";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/fordel-dommer/fjern-rase")
	public String postFjernRaseFraDommer(@PathVariable Long utstillingId,
	                                     Model model,
	                                     @RequestParam Long dommerPaameldingId,
	                                     @RequestParam String rase) {
		DommerPaamelding valgtDommer = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId).stream()
				.filter(dp -> dp.getId().equals(dommerPaameldingId))
				.findFirst()
				.orElseThrow();

		List<String> oppdatert = RaseStringHjelper.hentUtRaser(valgtDommer).stream()
				.filter(tildeltRase -> !tildeltRase.equalsIgnoreCase(rase))
				.toList();
		dommerService.fordelRaserTilDommer(dommerPaameldingId, oppdatert);

		List<Due> duer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId).stream()
				.peek(due -> {
					if (due.getTildeltDommer() != null
							&& due.getTildeltDommer().getId().equals(dommerPaameldingId)
							&& due.getRase() != null
							&& due.getRase().equalsIgnoreCase(rase)) {
						due.setTildeltDommer(null);
					}
				})
				.toList();
		dueService.saveAll(duer);

		settDommerFordelingModel(model, utstillingId);
		return "admin/admin_fragments :: dommerFordeling";
	}

	private void settDommerFordelingModel(Model model, Long utstillingId) {
		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(utstillingId));
		List<DommerPaamelding> dommerListe = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId);
		List<Due> paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		Map<String, Long> raseAntall = byggRaseAntall(paameldteDuer);
		model.addAttribute("dommerListe", dommerListe);
		model.addAttribute("paameldteDuer", paameldteDuer);
		model.addAttribute("raseAntall", raseAntall);
		model.addAttribute("raseAntallNormalisert", byggNormalisertRaseAntall(raseAntall));
		model.addAttribute("tilgjengeligeRaseAntall", byggTilgjengeligeRaseAntall(raseAntall, dommerListe));
	}

	private Map<String, Long> byggRaseAntall(List<Due> duer) {
		return duer.stream()
				.collect(Collectors.groupingBy(
						due -> due.getRase() == null || due.getRase().isBlank() ? "Ukjent" : due.getRase(),
						LinkedHashMap::new,
						Collectors.counting()
				));
	}

	private Map<String, Long> byggTilgjengeligeRaseAntall(Map<String, Long> raseAntall, List<DommerPaamelding> dommerListe) {
		Set<String> tildelteRaser = new HashSet<>();
		dommerListe.forEach(dp -> tildelteRaser.addAll(RaseStringHjelper.hentUtRaser(dp)));

		return raseAntall.entrySet().stream()
				.filter(entry -> tildelteRaser.stream().noneMatch(r -> r.equalsIgnoreCase(entry.getKey())))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(a, b) -> a,
						LinkedHashMap::new
				));
	}

	private Map<String, Long> byggNormalisertRaseAntall(Map<String, Long> raseAntall) {
		return raseAntall.entrySet().stream()
				.collect(Collectors.toMap(
						entry -> entry.getKey().trim().toLowerCase(),
						Map.Entry::getValue,
						Long::sum,
						LinkedHashMap::new
				));
	}

	@HxRequest
	@GetMapping("/{utstillingId}/raseSortering")
	public String getRaseSorteringHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                   RedirectAttributes redirectAttributes) {
		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(utstillingId));
		model.addAttribute("paameldteRaser", dueService.hentRaserPaameldtUtstilling(utstillingId));
		return "admin/admin_fragments :: raseSortering";
	}

	@HxRequest
	@PostMapping("/sorterRaser/{utstillingId}")
	public String postRaseSorteringHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                    RedirectAttributes redirectAttributes, @RequestParam String sorterteRaser) {
		utstillingService.oppdaterSorterteRaser(utstillingId, sorterteRaser);

		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(utstillingId));
		model.addAttribute("paameldteRaser", dueService.hentRaserPaameldtUtstilling(utstillingId));
		return "admin/admin_fragments :: raseSortering";
	}

	@HxRequest
	@GetMapping("/{utstillingId}/burnummerGenerering")
	public String getGenererBurnumreHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                     RedirectAttributes redirectAttributes) {
		Utstilling utstilling    = utstillingService.finnUtstillingMedId(utstillingId);
		List<Due>  paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("paameldteDuer", paameldteDuer);
		return "admin/admin_fragments :: genererBurnumre";
	}

	@HxRequest
	@PostMapping("/genererBurnumre/{utstillingId}")
	public String postGenererBurnumreHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                      RedirectAttributes redirectAttributes) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		utstillingService.genererBurnumre(utstilling);
		List<Due> paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("paameldteDuer", paameldteDuer);
		return "admin/admin_fragments :: genererBurnumre";
	}

	@GetMapping("/genererkatalogdata/{utstillingId}")
	public String genererKatalogData(@PathVariable Long utstillingId, Model model, HttpSession session) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		Map<String, List<String>> raseFargerMap = utstillingService.hentFargeForRaseFraUtstilling(utstilling);
		List<String> raser = dueService.hentRaserPaameldtUtstilling(utstillingId);
		List<DommerPaamelding> dommere = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId);
		List<Bruker> sortertUtstillere = utstillingService.hentSortertListeAvUtstillereFraUtstilling(utstillingId);
		Map<Long, List<String>> utstillerRaser = utstillingService.hentUtstillereSineRaser(utstillingId);
		Map<String, List<String>> raseVarianter = utstillingService.hentVarianterForRase(utstillingId);
		Map<String, List<String>> variantFarger = utstillingService.hentFargerForVarianter(utstillingId);


		model.addAttribute("raseFarger", raseFargerMap);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("raseliste", raser);
		model.addAttribute("sortedUtstillere", sortertUtstillere);
		model.addAttribute("utstillerRaser", utstillerRaser);
		model.addAttribute("dommerListe", dommere);
		model.addAttribute("raseVarianter", raseVarianter);
		model.addAttribute("variantFarger", variantFarger);
		return "admin/admin_fragments :: genererkatalogdata";
	}

	@GetMapping("/{utstillingId}/katalog-pdf")
	public ResponseEntity<byte[]> hentKatalogPdf(@PathVariable Long utstillingId) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		byte[] pdf = katalogPdfService.genererKatalogPdf(utstillingId);

		String filnavn = "katalog-" + lagFilnavn(utstilling.getTittel()) + ".pdf";
		ContentDisposition disposition = ContentDisposition.attachment().filename(filnavn).build();

		return ResponseEntity.ok()
		                     .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
		                     .contentType(MediaType.APPLICATION_PDF)
		                     .body(pdf);
	}

	@GetMapping("/katalog-pdf")
	public String getKatalogPdfBuilder(@RequestParam(required = false) Long utstillingId,
	                                  @RequestParam(required = false) String forsideTittel,
	                                  @RequestParam(required = false) String forsideArrangor,
	                                  @RequestParam(required = false) String forsideSted,
	                                  @RequestParam(required = false) String forsideDato,
	                                  @RequestParam(required = false) String katalogKommentar,
	                                  @RequestParam(required = false) String dommerlisteOverskrift,
	                                  @RequestParam(required = false) String bisOverskrift,
	                                  @RequestParam(required = false) String mestereOverskrift,
	                                  @RequestParam(required = false) String gruppevinnereOverskrift,
	                                  @RequestParam(required = false) String championsOverskrift,
	                                  @RequestParam(required = false) String utstillereOverskrift,
	                                  Model model) {
		List<Utstilling> tilgjengeligeUtstillinger = hentUtstillingerForKatalogvalg();
		model.addAttribute("katalogUtstillinger", tilgjengeligeUtstillinger);

		Long valgtUtstillingId = finnValgtUtstillingId(tilgjengeligeUtstillinger, utstillingId);
		model.addAttribute("katalogValgtUtstillingId", valgtUtstillingId);

		if (valgtUtstillingId != null) {
			Utstilling valgtUtstilling = utstillingService.finnUtstillingMedId(valgtUtstillingId);
			KatalogPdfService.KatalogPdfRedigering standard = katalogPdfService.standardRedigering(valgtUtstilling);
			KatalogPdfService.KatalogPdfRedigering redigering = new KatalogPdfService.KatalogPdfRedigering(
				verdiEllerStandard(forsideTittel, standard.forsideTittel()),
				verdiEllerStandard(forsideArrangor, standard.forsideArrangor()),
				verdiEllerStandard(forsideSted, standard.forsideSted()),
				verdiEllerStandard(forsideDato, standard.forsideDato()),
				verdiEllerStandard(katalogKommentar, standard.katalogKommentar()),
				verdiEllerStandard(dommerlisteOverskrift, standard.dommerlisteOverskrift()),
				verdiEllerStandard(bisOverskrift, standard.bisOverskrift()),
				verdiEllerStandard(mestereOverskrift, standard.mestereOverskrift()),
				verdiEllerStandard(gruppevinnereOverskrift, standard.gruppevinnereOverskrift()),
				verdiEllerStandard(championsOverskrift, standard.championsOverskrift()),
				verdiEllerStandard(utstillereOverskrift, standard.utstillereOverskrift())
			);

			model.addAttribute("katalogRedigering", redigering);
			model.addAttribute("katalogPreviewHtml", katalogPdfService.genererKatalogHtml(valgtUtstillingId, redigering));
		}

		model.addAttribute("fragment", "katalogPdfBuilder");
		return "admin/admin";
	}

	@PostMapping("/katalog-pdf/download")
	public ResponseEntity<byte[]> postKatalogPdfDownload(@RequestParam Long utstillingId,
	                                                    @RequestParam String forsideTittel,
	                                                    @RequestParam String forsideArrangor,
	                                                    @RequestParam String forsideSted,
	                                                    @RequestParam String forsideDato,
	                                                    @RequestParam String katalogKommentar,
	                                                    @RequestParam String dommerlisteOverskrift,
	                                                    @RequestParam String bisOverskrift,
	                                                    @RequestParam String mestereOverskrift,
	                                                    @RequestParam String gruppevinnereOverskrift,
	                                                    @RequestParam String championsOverskrift,
	                                                    @RequestParam String utstillereOverskrift) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		KatalogPdfService.KatalogPdfRedigering redigering = new KatalogPdfService.KatalogPdfRedigering(
			forsideTittel,
			forsideArrangor,
			forsideSted,
			forsideDato,
			katalogKommentar,
			dommerlisteOverskrift,
			bisOverskrift,
			mestereOverskrift,
			gruppevinnereOverskrift,
			championsOverskrift,
			utstillereOverskrift
		);
		byte[] pdf = katalogPdfService.genererKatalogPdf(utstillingId, redigering);

		String filnavn = "katalog-" + lagFilnavn(utstilling.getTittel()) + ".pdf";
		ContentDisposition disposition = ContentDisposition.attachment().filename(filnavn).build();

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdf);
	}

	private String lagFilnavn(String tittel) {
		if (tittel == null || tittel.isBlank()) {
			return "utstilling";
		}
		return tittel
				.trim()
				.toLowerCase()
				.replaceAll("[^a-z0-9-\\s]", "")
				.replaceAll("\\s+", "-");
	}

	private Long finnValgtUtstillingId(List<Utstilling> utstillinger, Long utstillingId) {
		if (utstillingId != null) {
			return utstillingId;
		}
		if (utstillinger.isEmpty()) {
			return null;
		}
		return utstillinger.get(0).getId();
	}

	private List<Utstilling> hentUtstillingerForKatalogvalg() {
		return java.util.stream.Stream.concat(
				utstillingService.finnTidligereUtstillinger().stream(),
				utstillingService.finnIkkeTidligereUtstillinger().stream()
		)
		.distinct()
		.sorted(Comparator.comparing(
				(Utstilling u) -> u.getDatoRange() != null ? u.getDatoRange().getStartDate() : null,
				Comparator.nullsLast(Comparator.naturalOrder())
		).reversed())
		.toList();
	}

	private String verdiEllerStandard(String verdi, String standard) {
		if (verdi == null || verdi.isBlank()) {
			return standard;
		}
		return verdi.trim();
	}

	@HxRequest
	@GetMapping("/{utstillingId}/bulkdueendring")
	public String getBulkDueEndringHtmx(@PathVariable Long utstillingId, Model model, HttpSession session) {
		Utstilling utstilling    = utstillingService.finnUtstillingMedId(utstillingId);
		List<Due>  paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("paameldteDuer", paameldteDuer);
		return "admin/admin_fragments :: bulkDueEndring";
	}

	@HxRequest
	@GetMapping("/{utstillingId}/bulkendre/{felt}")
	public String getBulkDueFeltEndringHtmx(@PathVariable Long utstillingId, @PathVariable String felt, Model model,
	                                     HttpSession session, RedirectAttributes redirectAttributes) {
		if (felt == null || !felt.matches("rase|farge|variant")) {
			Utstilling utstilling    = utstillingService.finnUtstillingMedId(utstillingId);
			List<Due>  paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
			model.addAttribute("utstilling", utstilling);
			model.addAttribute("paameldteDuer", paameldteDuer);
			return "admin/admin_fragments :: bulkDueEndring";
		}
		Utstilling utstilling    = utstillingService.finnUtstillingMedId(utstillingId);
		List<Due>  paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		switch (felt) {
			case "rase" -> {
				paameldteDuer.sort((d1, d2) -> d1.getRase().compareToIgnoreCase(d2.getRase()));
			}
			case "farge" -> {
				paameldteDuer.sort((d1, d2) -> d1.getFarge().compareToIgnoreCase(d2.getFarge()));
			}
			case "variant" -> {
				paameldteDuer.sort((d1, d2) -> d1.getVariant().compareToIgnoreCase(d2.getVariant()));
			}
		}


		model.addAttribute("utstilling", utstilling);
		model.addAttribute("paameldteDuer", paameldteDuer);
		model.addAttribute("felt", felt);
		return "admin/admin_fragments :: bulkEditSelection";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/bulkendre/{felt}")
	public String postBulkDueFeltEndringHtmx(@PathVariable Long utstillingId, @PathVariable String felt,
	                                         Model model, HttpSession session, RedirectAttributes redirectAttributes,
	                                         @RequestParam List<Long> dueId, @RequestParam(required = false) String nyVerdi) {
		switch (felt) {
			case "rase" -> dueService.endreRasePaDuer(nyVerdi, dueId);
			case "farge" -> dueService.endreFargePaDuer(nyVerdi, dueId);
			case "variant" -> dueService.endreVariantPaDuer(nyVerdi, dueId);
			default -> {}
		}

		Utstilling utstilling    = utstillingService.finnUtstillingMedId(utstillingId);
		List<Due>  paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("paameldteDuer", paameldteDuer);
		model.addAttribute("felt", felt);
		return "admin/admin_fragments :: bulkDueEndring";
	}

	@HxRequest
	@PostMapping("/aktiverUtstilling/{utstillingId}")
	public String postAktiverUtstillingHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                    RedirectAttributes redirectAttributes) {
		Utstilling utstilling = utstillingService.setAktivUtstilling(utstillingId);
		model.addAttribute("utstilling", utstilling);
		return "admin/admin_fragments :: adminUtstilling";
	}

	@HxRequest
	@PostMapping("/deaktiverUtstilling/{utstillingId}")
	public String postDeaktiverUtstillingHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                        RedirectAttributes redirectAttributes) {
		utstillingService.fjernAktivUtstilling(); // denne metoden setter aktiv = false for alle utstillinger
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		model.addAttribute("utstilling", utstilling);
		return "admin/admin_fragments :: adminUtstilling";
	}

	@GetMapping("/duekatalog")
	public String getDuekatalog(Model model) {
		leggTilDuekatalogModel(model);
		return "admin/admin_duekatalog";
	}

	@PostMapping("/duekatalog/gruppe")
	public String postLeggTilGruppe(@RequestParam String navn) {
		dueKatalogService.opprettGruppe(navn);
		return "redirect:/admin/duekatalog";
	}

	@PostMapping("/duekatalog/rase")
	public String postLeggTilRase(@RequestParam Long gruppeId, @RequestParam String navn) {
		dueKatalogService.opprettRase(gruppeId, navn);
		return "redirect:/admin/duekatalog";
	}

	@PostMapping("/duekatalog/farge")
	public String postLeggTilFarge(@RequestParam String navn) {
		dueKatalogService.opprettFarge(navn);
		return "redirect:/admin/duekatalog";
	}

	@PostMapping("/duekatalog/variant")
	public String postLeggTilVariant(@RequestParam String navn) {
		dueKatalogService.opprettVariant(navn);
		return "redirect:/admin/duekatalog";
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

	private void leggTilDuekatalogModel(Model model) {
		model.addAttribute("grupper", dueKatalogService.finnAlleGrupper());
		model.addAttribute("raser", dueKatalogService.finnAlleRaser());
		model.addAttribute("farger", dueKatalogService.finnAlleFarger());
		model.addAttribute("varianter", dueKatalogService.finnAlleVarianter());
	}
}
