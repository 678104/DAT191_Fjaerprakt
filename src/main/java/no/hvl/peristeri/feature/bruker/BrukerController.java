package no.hvl.peristeri.feature.bruker;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.feature.paamelding.PaameldingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/bruker")
public class BrukerController {
	private static final String navLocation = "bruker";

	private final Logger logger = LoggerFactory.getLogger(BrukerController.class);

	private final BrukerService     brukerService;
	private final PaameldingService paameldingService;

	@GetMapping
	public String bruker(@AuthenticationPrincipal Bruker bruker, Model model, HttpSession session) {

		Map<String, List<Paamelding>> paameldinger = paameldingService.hentBrukerSinePaameldinger(bruker);
		model.addAttribute("kommendePaameldinger", paameldinger.get("kommende"));
		model.addAttribute("tidligerePaameldinger", paameldinger.get("tidligere"));

		return "bruker/bruker";
	}

	@HxRequest
	@GetMapping
	public String getBrukerHtmx() {
		return "bruker/bruker :: brukerInfo";
	}

	@GetMapping("/paamelding/{paameldingId}")
	public String getPaamledingInfo(@PathVariable("paameldingId") Long paameldingId, Model model, HttpSession session) {
		Paamelding paamelding = paameldingService.hentPaamelding(paameldingId);
		model.addAttribute("paamelding", paamelding);

		return "bruker/bruker_paamelding";
	}

	@HxRequest
	@GetMapping("/redigerBrukerInfo")
	public String getRedigerBrukerHtmx(Model model, HttpSession session) {
		return "bruker/bruker_fragments :: redigerBrukerInfo";
	}

	@HxRequest
	@PostMapping("/redigerBrukerInfo")
	public String postRedigerBrukerHtmx(@AuthenticationPrincipal Bruker bruker, @RequestParam String fornavn,
	                                    @RequestParam String etternavn, @RequestParam String telefon,
	                                    @RequestParam String epost, @RequestParam String adresse,
	                                    @RequestParam String postnummer, @RequestParam String poststed,
	                                    @RequestParam String forening, Model model, HttpSession session) {
		Bruker lagretBruker = brukerService.oppdaterBrukerInfo(bruker.getId(), fornavn, etternavn, telefon, epost,
				adresse,
				postnummer,
				poststed, forening);
		brukerService.refreshUserAuthentication(lagretBruker);
		model.addAttribute("bruker", lagretBruker);
		return "bruker/bruker :: brukerInfo";
	}

	@HxRequest
	@GetMapping("/endrePassord")
	public String getEndrePassordHtmx(Model model, HttpSession session) {
		return "bruker/bruker_fragments :: endrePassord";
	}

	@HxRequest
	@PostMapping("/endrePassord")
	public String postEndrePassordHtmx(@AuthenticationPrincipal Bruker bruker, @RequestParam String passord,
	                                   @RequestParam String nyttPassord, Model model, HttpSession session) {
		if (passord.length() < 4) {
			model.addAttribute("feilmelding", "Passord må være minst 4 tegn langt");
			return "bruker/bruker_fragments :: endrePassord";
		}

		if (!brukerService.endrePassord(bruker, passord, nyttPassord)) {
			model.addAttribute("feilmelding", "Passord kan ikke være likt som det gamle passordet");
			return "bruker/bruker_fragments :: endrePassord";
		}

		return "bruker/bruker :: brukerInfo";
	}


	@HxRequest
	@GetMapping("/paamelding/{paameldingId}/ringnumre")
	public String getRingNumreHtmx(@PathVariable("paameldingId") Long paameldingId, Model model, HttpSession session) {
		Paamelding paamelding = paameldingService.hentPaamelding(paameldingId);
		model.addAttribute("paamelding", paamelding);

		return "bruker/bruker_fragments :: redigerRingnumre";
	}

	@HxRequest
	@GetMapping("/paamelding/{paameldingId}/dueliste")
	public String getDueListeHtmx(@PathVariable("paameldingId") Long paameldingId, Model model, HttpSession session) {
		Paamelding paamelding = paameldingService.hentPaamelding(paameldingId);
		model.addAttribute("paamelding", paamelding);

		return "bruker/bruker_paamelding :: #due-liste";
	}

	@HxRequest
	@GetMapping("/paamelding/{paameldingId}/resultater")
	public String getResultaterHtmx(@PathVariable("paameldingId") Long paameldingId, Model model, HttpSession session) {
		Paamelding paamelding = paameldingService.hentPaamelding(paameldingId);
		model.addAttribute("paamelding", paamelding);

		return "bruker/bruker_resultatliste";
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

	@ModelAttribute("bruker")
	public Bruker bruker(@AuthenticationPrincipal Bruker bruker) {
		return bruker;
	}

}
