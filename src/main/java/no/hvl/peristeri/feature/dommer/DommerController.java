package no.hvl.peristeri.feature.dommer;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/dommer")
@RequiredArgsConstructor
public class DommerController {
	private static final String navLocation = "dommer";

	private final Logger logger = LoggerFactory.getLogger(DommerController.class);

	private final DommerService dommerService;

	@GetMapping
	public String dommer(@AuthenticationPrincipal Bruker bruker, Model model, HttpSession session) {
		model.addAttribute("duer", dommerService.finnDuerDommerSkalBedomme(bruker));
		return "dommer/dommer";
	}

	@HxRequest
	@GetMapping("/liste")
	public String dommerListeHtmx(@RequestParam (required = false) Integer filter, @AuthenticationPrincipal Bruker bruker, Model model, HttpSession session) {
		List<Due> duer;
		if (filter == null) {
			duer = dommerService.finnDuerDommerSkalBedomme(bruker);
		} else {
			duer = List.of(dommerService.finnDueDommerSkalBedommeMedBurnummer(bruker, filter));
		}
		model.addAttribute("duer", duer);
		return "dommer/dommer_fragments :: dueliste";
	}

	@HxRequest
	@GetMapping("/bedom")
	public String bedomDommerHtmx(@RequestParam Long dueId, Model model) {
		Due due = dommerService.hentDueMedId(dueId);

		Bedommelse bedommelse = due.getBedommelse();
		if(bedommelse == null) {
			bedommelse = new Bedommelse();
		}
		model.addAttribute("due", due);
		model.addAttribute("bedommelse", bedommelse);

		return "dommer/dommer_fragments :: dommerBedommelse";
	}

	@HxRequest
	@PostMapping("/bedom")
	public String lagreBedomming(@RequestParam Long dueId, @ModelAttribute Bedommelse bedommelse, Model model,
	                             @AuthenticationPrincipal Bruker bruker) {
		dommerService.lagreBedommelse(dueId, bedommelse, bruker);
		model.addAttribute("duer", dommerService.finnDuerDommerSkalBedomme(bruker));
		return "dommer/dommer_fragments :: dueliste";
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

}
