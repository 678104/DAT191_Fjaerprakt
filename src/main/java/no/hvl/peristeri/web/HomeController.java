package no.hvl.peristeri.web;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@RequiredArgsConstructor
@Controller
public class HomeController {
	private static final String navLocation = "home";

	private final Logger logger = LoggerFactory.getLogger(HomeController.class);

	private final UtstillingService utstillingService;

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("kommendeUtstillinger", utstillingService.finnKommendeUtstillinger());
		model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
		Utstilling aktivUtstilling = utstillingService.finnAktivUtstilling();
		if (aktivUtstilling != null) {
			model.addAttribute("aktivUtstilling", aktivUtstilling);
		}
		return "index";
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

}
