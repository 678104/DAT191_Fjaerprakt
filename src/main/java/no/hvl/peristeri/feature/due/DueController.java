package no.hvl.peristeri.feature.due;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

@RequiredArgsConstructor
@Controller
@RequestMapping("/due")
public class DueController {
	private final Logger logger = LoggerFactory.getLogger(DueController.class);

	private final DueService dueService;

	@HxRequest
	@PostMapping("/endreRingnummer/{id}/rad")
	public View oppdaterRingnummerHtmxFraBrukerPaamelding(@PathVariable Long id, @RequestParam String lopenr,
	                                                      @RequestParam String aarstall, Model model) {

		Due due = dueService.oppdaterRingnummer(id, lopenr, aarstall);
		model.addAttribute("due", due);
		return FragmentsRendering
				.with("bruker/bruker_fragments :: lagretRad")
				.build();
	}

	@HxRequest
	@GetMapping("/{id}/bedommelse")
	public String getBedommelseHtmx(@PathVariable Long id, Model model) {
		Due due = dueService.finnDueMedId(id);
		model.addAttribute("due", due);
		return "due/due_bedommelse";
	}

	@HxRequest
	@PostMapping("/adminEndreDue")
	public String oppdaterDueHtmxFraAdmin(@RequestParam Long id, @RequestParam String rase,
	                              @RequestParam(required = false) String farge, @RequestParam(required = false) String variant, Model model) {
		Due saved = dueService.oppdaterDueInfo(id, rase, farge, variant);
		model.addAttribute("due", saved);
		return "admin/admin_fragments :: bulkEditRad";
	}
}
