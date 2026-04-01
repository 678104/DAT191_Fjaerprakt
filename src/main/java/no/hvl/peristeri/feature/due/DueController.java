package no.hvl.peristeri.feature.due;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

@RequiredArgsConstructor
@Controller
@RequestMapping("/due")
public class DueController {
	private final DueService dueService;
	private final DueLookupService dueLookupService;

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
	public String oppdaterDueHtmxFraAdmin(@RequestParam Long id, @RequestParam Long raseId,
	                              @RequestParam Long fargeId, @RequestParam Long variantId, Model model) {
		Due saved = dueService.oppdaterDueInfo(id, raseId, fargeId, variantId);
		model.addAttribute("due", saved);
		model.addAttribute("raser", dueLookupService.hentAlleRaser());
		model.addAttribute("farger", dueLookupService.hentAlleFarger());
		model.addAttribute("varianter", dueLookupService.hentAlleVarianter());
		return "admin/admin_fragments :: bulkEditRad";
	}
}
