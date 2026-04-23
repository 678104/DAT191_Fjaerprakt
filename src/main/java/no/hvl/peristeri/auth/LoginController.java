package no.hvl.peristeri.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
public class LoginController {
	private final Logger logger = LoggerFactory.getLogger(LoginController.class);

	private static final String navLocation = "login";

	private final BrukerService brukerService;
	private final PasswordResetService passwordResetService;
	private final PasswordResetMailService passwordResetMailService;
	private final PasswordResetRateLimiter passwordResetRateLimiter;
	private final PasswordResetAuditService auditService;

	@GetMapping("/login")
	public String login(Model model, @RequestParam(required = false) String error, @RequestParam(required = false) String logout,
	                    @RequestParam(required = false) String resetSuccess) {
		if (error != null) {
			model.addAttribute("error", "Feil brukernavn/epost eller passord!");
			logger.info("Invalid username and password!");
		}
		if (logout != null) {
			model.addAttribute("melding", "Du har blitt logget ut!");
		}
		if (resetSuccess != null) {
			model.addAttribute("melding", "Passordet er oppdatert. Du kan logge inn nå.");
		}
		return "auth/login";
	}

	@GetMapping("/glemt-passord")
	public String glemtPassord(@RequestParam(required = false) String sent, Model model) {
		if (sent != null) {
			model.addAttribute("melding", "Hvis e-posten finnes, har vi sendt en lenke for passordbytte.");
		}
		return "auth/forgot-password";
	}

	@PostMapping("/glemt-passord")
	public String sendResetLenke(@RequestParam String epost, HttpServletRequest request, RedirectAttributes ra) {
		boolean allowed = passwordResetRateLimiter.isAllowed(epost, request.getRemoteAddr());
		if (allowed) {
			Optional<String> tokenOptional = passwordResetService.requestPasswordReset(epost);
			tokenOptional.ifPresent(token -> {
				String resetUrl = passwordResetMailService.createResetUrl(token);
				passwordResetMailService.sendPasswordResetEmail(epost, resetUrl);
			});
		} else {
			auditService.resetRateLimited(epost, request.getRemoteAddr());
		}
		ra.addAttribute("sent", "1");
		return "redirect:/glemt-passord";
	}

	@GetMapping("/reset-passord")
	public String resetPassordForm(@RequestParam(required = false) String token, Model model) {
		if (!passwordResetService.isResetTokenValid(token)) {
			model.addAttribute("error", "Lenken er ugyldig eller har utløpt.");
			model.addAttribute("tokenInvalid", true);
			return "auth/reset-password";
		}
		model.addAttribute("token", token);
		return "auth/reset-password";
	}

	@PostMapping("/reset-passord")
	public String resetPassord(@RequestParam String token,
	                           @RequestParam String passord,
	                           @RequestParam String bekreftPassord,
	                           Model model,
	                           RedirectAttributes ra) {
		if (!passord.equals(bekreftPassord)) {
			model.addAttribute("error", "Passordene er ikke like.");
			model.addAttribute("token", token);
			return "auth/reset-password";
		}

		if (passord.length() < 4) {
			model.addAttribute("error", "Passord må være minst 4 tegn langt.");
			model.addAttribute("token", token);
			return "auth/reset-password";
		}

		boolean updated = passwordResetService.resetPassword(token, passord);
		if (!updated) {
			model.addAttribute("error", "Lenken er ugyldig eller har utløpt.");
			model.addAttribute("tokenInvalid", true);
			return "auth/reset-password";
		}

		ra.addAttribute("resetSuccess", "1");
		return "redirect:/login";
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
