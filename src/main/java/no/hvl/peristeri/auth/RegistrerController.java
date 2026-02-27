package no.hvl.peristeri.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequiredArgsConstructor
@Controller
@RequestMapping("/registrer")
public class RegistrerController {
	private static final String        navLocation = "login";
	private final        Logger        logger      = LoggerFactory.getLogger(RegistrerController.class);
	private final        BrukerService brukerService;

	@GetMapping
	public String getRegistrer(Model model, HttpSession session) {
		model.addAttribute("nyBruker",
				session.getAttribute("nyBruker") == null ? new Bruker() : session.getAttribute("nyBruker"));
		return "auth/registrer";
	}

	@PostMapping
	public String postRegistrer(@ModelAttribute @Valid Bruker nyBruker, @RequestParam String passord,
	                            Model model, RedirectAttributes ra, BindingResult bindingResult, HttpSession session) {

		if (bindingResult.hasErrors()) {
			logger.info("Valideringsfeil: {}", bindingResult.getAllErrors());
			session.setAttribute("nyBruker", nyBruker);
			model.addAttribute("feilmelding",
					"Mangler obligatoriske felt. Vennligst fyll inn alle obligatoriske felt.");
			return "auth/registrer";
		}

		if (passord.length() < 4) {
			session.setAttribute("nyBruker", nyBruker);
			model.addAttribute("feilmelding", "Passord må være minst 4 tegn langt");
			return "auth/registrer";
		}

		if (brukerService.sjekkOmEpostErBrukt(nyBruker.getEpost())) {
			session.setAttribute("nyBruker", nyBruker);
			model.addAttribute("feilmelding", "Eposten er allerede i bruk. Sjekk om du allerede har en brukerkonto, eller bruk en annen epost.");
			return "auth/registrer";
		}
		session.removeAttribute("nyBruker");
		brukerService.lagreBrukerMedPassord(nyBruker, passord);
		ra.addFlashAttribute("melding", "Bruker registrert. Du kan nå logge inn.");
		ra.addFlashAttribute("epost", nyBruker.getEpost());
		return "redirect:/login";
	}


	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}
}
