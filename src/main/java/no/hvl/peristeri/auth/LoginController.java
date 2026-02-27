package no.hvl.peristeri.auth;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class LoginController {
	private final Logger logger = LoggerFactory.getLogger(LoginController.class);

	private static final String navLocation = "login";

	private final BrukerService brukerService;

	@GetMapping("/login")
	public String login(Model model, @RequestParam(required = false) String error, @RequestParam(required = false) String logout) {
		if (error != null) {
			model.addAttribute("error", "Feil brukernavn/epost eller passord!");
			logger.info("Invalid username and password!");
		}
		if (logout != null) {
			model.addAttribute("melding", "Du har blitt logget ut!");
		}
		return "auth/login";
	}


	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

	@ModelAttribute("brukere")
	public List<Bruker> brukere() {
		return brukerService.getBrukere();
	}
}
