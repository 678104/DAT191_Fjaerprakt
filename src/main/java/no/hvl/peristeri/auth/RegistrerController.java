package no.hvl.peristeri.auth;

import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
	private final AuthenticationManager authenticationManager;

	@GetMapping
	public String getRegistrer(Model model, HttpSession session) {
		model.addAttribute("nyBruker",
				session.getAttribute("nyBruker") == null ? new Bruker() : session.getAttribute("nyBruker"));
		return "auth/registrer";
	}

	@PostMapping
	public String postRegistrer(@ModelAttribute @Valid Bruker nyBruker, @RequestParam String passord,
	                            Model model, RedirectAttributes ra, BindingResult bindingResult, HttpSession session,
	                            HttpServletRequest request) {

		if (bindingResult.hasErrors()) {
			logger.info("Valideringsfeil: {}", bindingResult.getAllErrors());
			session.setAttribute("nyBruker", nyBruker);
			model.addAttribute("feilmelding",
					"Mangler obligatoriske felt. Vennligst fyll inn alle obligatoriske felt.");
			return "auth/registrer";
		}

		if (!isValidEmail(nyBruker.getEpost())) {
			session.setAttribute("nyBruker", nyBruker);
			model.addAttribute("feilmelding", "Eposten er ikke gyldig.");
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
		Bruker savedUser = brukerService.lagreBrukerMedPassord(nyBruker, passord);

		// Automatically log in the new user
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(savedUser.getEpost(), passord)
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// Manually set the authentication in the session
		request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

		ra.addFlashAttribute("melding", "Bruker registrert og logget inn.");
		return "redirect:/";
	}

	private boolean isValidEmail(String email) {
		String emailRegex = "^[a-zA-Z0-9._\\-æøåÆØÅ]+@[a-zA-Z0-9.-]+\\.[a-zA-ZæøåÆØÅ]{2,4}$";
		Pattern pattern = Pattern.compile(emailRegex);
		return pattern.matcher(email).matches();
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}
}
