package no.hvl.peristeri.feature.admin;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxReswap;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import no.hvl.peristeri.util.RaseStringHjelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
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

	@GetMapping("/tildel-dommer-rolle-liste")
	public String getTildelDommerRolleListe(Model model) {
		model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
		model.addAttribute("fragment", "tildelDommerRolleListe");
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
		return "admin/admin_fragments :: tildelDommerRolle";
	}

	@HxRequest
	@GetMapping("/{id}/tildel-dommer-rolle/brukere")
	public String getTildelDommerRolleBrukereHtmx(@PathVariable Long id, @RequestParam(required = false) String sok,
	                                             Model model) {
		model.addAttribute("brukere", sok == null || sok.isBlank() ? List.of() : brukerService.finnBrukere(sok));
		model.addAttribute("sok", sok);
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

		if (brukerId == null) {
			model.addAttribute("brukerFeil", "Velg bruker å tildele rolle");
			htmxResponse.setReswap(HtmxReswap.outerHtml());
			htmxResponse.setRetarget("main-content");
			return "admin/admin_fragments :: tildelDommerRolle";
		}

		Bruker bruker = brukerService.hentBrukerMedId(brukerId);
		bruker.leggTilRolle(Rolle.DOMMER);
		brukerService.lagreBruker(bruker);
		List<DommerPaamelding> eksisterende = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId).stream()
				.filter(dp -> dp.getDommer() != null && dp.getDommer().getId().equals(brukerId))
				.toList();
		if (eksisterende.isEmpty()) {
			DommerPaamelding dommerPaamelding = new DommerPaamelding(utstilling, bruker);
			dommerService.lagreDommerPaaMelding(dommerPaamelding);
		}
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(utstillingId));
		return "admin/admin_fragments :: tildelDommerRolle";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/fjern-dommer/{dommerPaameldingId}")
	public String postFjernDommer(@PathVariable Long utstillingId, @PathVariable Long dommerPaameldingId, 
	                               Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		dommerService.fjernDommerPaamelding(dommerPaameldingId);
		
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		model.addAttribute("utstilling", utstilling);
		model.addAttribute("brukere", List.of());
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(utstillingId));
		return "admin/admin_fragments :: tildelDommerRolle";
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

	@PostMapping("/duekatalog/gruppe/{id}/slett")
	public String postSlettGruppe(@PathVariable Long id) {
		dueKatalogService.slettGruppe(id);
		return "redirect:/admin/duekatalog";
	}

	@PostMapping("/duekatalog/rase/{id}/slett")
	public String postSlettRase(@PathVariable Long id) {
		dueKatalogService.slettRase(id);
		return "redirect:/admin/duekatalog";
	}

	@PostMapping("/duekatalog/farge/{id}/slett")
	public String postSlettFarge(@PathVariable Long id) {
		dueKatalogService.slettFarge(id);
		return "redirect:/admin/duekatalog";
	}

	@PostMapping("/duekatalog/variant/{id}/slett")
	public String postSlettVariant(@PathVariable Long id) {
		dueKatalogService.slettVariant(id);
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
