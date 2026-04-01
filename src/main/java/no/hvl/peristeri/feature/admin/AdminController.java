package no.hvl.peristeri.feature.admin;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import no.hvl.peristeri.feature.dommer.DommerPaamelding;
import no.hvl.peristeri.feature.dommer.DommerService;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.due.DueLookupService;
import no.hvl.peristeri.feature.due.DueService;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminController {
	private static final String navLocation = "admin";

	private final Logger logger = LoggerFactory.getLogger(AdminController.class);

	private final BrukerService     brukerService;
	private final UtstillingService utstillingService;
	private final DommerService     dommerService;
	private final DueService        dueService;
	private final DueLookupService  dueLookupService;

	@GetMapping
	public String getAdmin(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
		model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
		return "admin/admin";
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
	@GetMapping("/{id}/dommerRegistrering")
	public String getDommerRegistreringHtmx(@PathVariable Long id, Model model, HttpSession session,
	                                        RedirectAttributes redirectAttributes) {
		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(id));
		return "admin/admin_fragments :: dommerRegistrering";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/registrerDommer")
	public String postRegistrerDommer(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                  RedirectAttributes redirectAttributes, @ModelAttribute @Valid Bruker dommer, @RequestParam String passord) {
		dommerService.lagreDommerPaamelding(dommer, utstillingId, passord);

		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(utstillingId));
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(utstillingId));
		return "admin/admin_fragments :: dommerListe";
	}

	@HxRequest
	@GetMapping("/{utstillingId}/dommerFordeling")
	public String getDommerFordelingHtmx(@PathVariable Long utstillingId, Model model, HttpSession session,
	                                     RedirectAttributes redirectAttributes) {
		model.addAttribute("raser", dueService.hentRaserPaameldtUtstilling(utstillingId));
		model.addAttribute("utstilling", utstillingService.finnUtstillingMedId(utstillingId));
		model.addAttribute("dommerListe", dommerService.finnDommerPaameldingerTilUtstilling(utstillingId));
		return "admin/admin_fragments :: dommerFordeling";
	}

	@HxRequest
	@PostMapping("/fordelRaser/{dommerPaameldingId}")
	public String postFordelDommerHtmx(Model model, HttpSession session,
	                                   RedirectAttributes redirectAttributes, @RequestParam List<String> raser,
	                                   @PathVariable Long dommerPaameldingId) {
		DommerPaamelding dp = dommerService.fordelRaserTilDommer(dommerPaameldingId, raser);
		model.addAttribute("raser", dueService.hentRaserPaameldtUtstilling(dp.getUtstilling().getId()));
		model.addAttribute("dp", dp);
		return "admin/admin_fragments :: fordelRaserRad";
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
		model.addAttribute("raser", dueLookupService.hentAlleRaser());
		model.addAttribute("farger", dueLookupService.hentAlleFarger());
		model.addAttribute("varianter", dueLookupService.hentAlleVarianter());
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
			model.addAttribute("raser", dueLookupService.hentAlleRaser());
			model.addAttribute("farger", dueLookupService.hentAlleFarger());
			model.addAttribute("varianter", dueLookupService.hentAlleVarianter());
			return "admin/admin_fragments :: bulkDueEndring";
		}
		Utstilling utstilling    = utstillingService.finnUtstillingMedId(utstillingId);
		List<Due>  paameldteDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		switch (felt) {
			case "rase" -> {
				paameldteDuer.sort((d1, d2) -> String.valueOf(d1.getRase()).compareToIgnoreCase(String.valueOf(d2.getRase())));
			}
			case "farge" -> {
				paameldteDuer.sort((d1, d2) -> String.valueOf(d1.getFarge()).compareToIgnoreCase(String.valueOf(d2.getFarge())));
			}
			case "variant" -> {
				paameldteDuer.sort((d1, d2) -> String.valueOf(d1.getVariant()).compareToIgnoreCase(String.valueOf(d2.getVariant())));
			}
		}


		model.addAttribute("utstilling", utstilling);
		model.addAttribute("paameldteDuer", paameldteDuer);
		model.addAttribute("felt", felt);
		model.addAttribute("raser", dueLookupService.hentAlleRaser());
		model.addAttribute("farger", dueLookupService.hentAlleFarger());
		model.addAttribute("varianter", dueLookupService.hentAlleVarianter());
		return "admin/admin_fragments :: bulkEditSelection";
	}

	@HxRequest
	@PostMapping("/{utstillingId}/bulkendre/{felt}")
	public String postBulkDueFeltEndringHtmx(@PathVariable Long utstillingId, @PathVariable String felt,
	                                         Model model, HttpSession session, RedirectAttributes redirectAttributes,
	                                         @RequestParam List<Long> dueId, @RequestParam Long nyVerdi) {
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
		model.addAttribute("raser", dueLookupService.hentAlleRaser());
		model.addAttribute("farger", dueLookupService.hentAlleFarger());
		model.addAttribute("varianter", dueLookupService.hentAlleVarianter());
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

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}
}
