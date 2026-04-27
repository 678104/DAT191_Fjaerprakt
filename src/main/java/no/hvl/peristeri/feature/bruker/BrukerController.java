package no.hvl.peristeri.feature.bruker;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.feature.paamelding.PaameldingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
	private final BedommelseNotifikasjonService bedommelseNotifikasjonService;

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
	public String getPaamledingInfo(@AuthenticationPrincipal Bruker bruker,
	                                @PathVariable("paameldingId") Long paameldingId,
	                                Model model,
	                                HttpSession session) {
		Paamelding paamelding = hentEgenPaamelding(bruker, paameldingId);
		leggTilUlesteBedommelser(model, bruker);
		model.addAttribute("paamelding", paamelding);
		model.addAttribute("kanEndrePaamelding", kanEndrePaamelding(paamelding));

		return "bruker/bruker_paamelding";
	}

	@GetMapping("/paamelding/endre/{paameldingId}")
	public String gaaTilEndrePaamelding(@AuthenticationPrincipal Bruker bruker,
	                                    @PathVariable("paameldingId") Long paameldingId) {
		Paamelding paamelding = hentEgenPaamelding(bruker, paameldingId);
		if (!kanEndrePaamelding(paamelding)) {
			return "redirect:/bruker/paamelding/" + paameldingId;
		}
		return "redirect:/paamelding?utstillingId=" + paamelding.getUtstilling().getId() + "&paameldingId=" + paameldingId;
	}

	@GetMapping("/din-paamelding")
	public String gaaTilDinPaamelding(@AuthenticationPrincipal Bruker bruker) {
		Map<String, List<Paamelding>> paameldinger = paameldingService.hentBrukerSinePaameldinger(bruker);

		Long paameldingId = paameldinger.getOrDefault("kommende", List.of()).stream()
				.findFirst()
				.map(Paamelding::getId)
				.orElseGet(() -> paameldinger.getOrDefault("tidligere", List.of()).stream()
						.findFirst()
						.map(Paamelding::getId)
						.orElse(null));

		if (paameldingId == null) {
			return "redirect:/bruker";
		}

		return "redirect:/bruker/paamelding/" + paameldingId;
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
	public String getRingNumreHtmx(@AuthenticationPrincipal Bruker bruker,
	                               @PathVariable("paameldingId") Long paameldingId,
	                               Model model,
	                               HttpSession session) {
		Paamelding paamelding = hentEgenPaamelding(bruker, paameldingId);
		model.addAttribute("paamelding", paamelding);

		return "bruker/bruker_fragments :: redigerRingnumre";
	}

	@HxRequest
	@GetMapping("/paamelding/{paameldingId}/dueliste")
	public String getDueListeHtmx(@AuthenticationPrincipal Bruker bruker,
	                              @PathVariable("paameldingId") Long paameldingId,
	                              Model model,
	                              HttpSession session) {
		Paamelding paamelding = hentEgenPaamelding(bruker, paameldingId);
		leggTilUlesteBedommelser(model, bruker);
		model.addAttribute("paamelding", paamelding);
		model.addAttribute("kanEndrePaamelding", kanEndrePaamelding(paamelding));

		return "bruker/bruker_paamelding :: #due-liste";
	}

	@HxRequest
	@GetMapping("/paamelding/{paameldingId}/resultater")
	public String getResultaterHtmx(@AuthenticationPrincipal Bruker bruker,
	                                @PathVariable("paameldingId") Long paameldingId,
	                                Model model,
	                                HttpSession session) {
		Paamelding paamelding = hentEgenPaamelding(bruker, paameldingId);
		bedommelseNotifikasjonService.markerSomLestForPaamelding(bruker.getId(), paameldingId);
		leggTilUlesteBedommelser(model, bruker);
		model.addAttribute("paamelding", paamelding);

		return "bruker/bruker_resultatliste";
	}

	@HxRequest
	@GetMapping("/notifikasjoner/badge")
	public String getNotifikasjonerBadge(@AuthenticationPrincipal Bruker bruker, Model model) {
		leggTilUlesteBedommelser(model, bruker);
		return "fragments/navbar :: bedommelseNotifikasjonOob";
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

	@ModelAttribute("bruker")
	public Bruker bruker(@AuthenticationPrincipal Bruker bruker) {
		return bruker;
	}

	private Paamelding hentEgenPaamelding(Bruker bruker, Long paameldingId) {
		Paamelding paamelding = paameldingService.hentPaamelding(paameldingId);
		if (bruker == null || paamelding.getUtstiller() == null || !paamelding.getUtstiller().getId().equals(bruker.getId())) {
			throw new InvalidParameterException("paameldingId", "Du har ikke tilgang til denne påmeldingen.");
		}
		return paamelding;
	}

	private boolean kanEndrePaamelding(Paamelding paamelding) {
		if (paamelding == null || paamelding.getUtstilling() == null) {
			return false;
		}
		if (Boolean.TRUE.equals(paamelding.getUtstilling().getPaameldingAApnet())) {
			return true;
		}
		LocalDate frist = paamelding.getUtstilling().getPaameldingsFrist();
		return frist == null || !LocalDate.now().isAfter(frist);
	}

	private void leggTilUlesteBedommelser(Model model, Bruker bruker) {
		Long brukerId = bruker != null ? bruker.getId() : null;
		model.addAttribute("ulesteBedommelser", bedommelseNotifikasjonService.tellUleste(brukerId));
	}


}
