package no.hvl.peristeri.web;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.feature.paamelding.PaameldingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;



@Controller
@RequestMapping("/print")
@RequiredArgsConstructor
public class PrintController {
	private final Logger logger = LoggerFactory.getLogger(PrintController.class);

	private final PaameldingService paameldingService;

	@GetMapping("/paamelding/{id}")
	public String printPaamelding(@PathVariable Long id, Model model) {
		Paamelding paamelding = paameldingService.hentPaamelding(id);
		model.addAttribute("paamelding", paamelding);


		return "print/paamelding";
	}
}
