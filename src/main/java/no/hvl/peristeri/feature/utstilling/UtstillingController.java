package no.hvl.peristeri.feature.utstilling;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.Rolle;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.feature.paamelding.PaameldingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.FragmentsRendering;

import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/utstilling")
@Controller
@RequiredArgsConstructor
public class UtstillingController {
	private static final String navLocation = "utstilling";

	private final Logger logger = LoggerFactory.getLogger(UtstillingController.class);

	private final UtstillingService utstillingService;
	private final PaameldingService paameldingService;

	@GetMapping
	public String utstilling(Model model) {
		model.addAttribute("fragment", "liste");
		model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
		model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
		return "utstilling/utstilling";
	}

	@HxRequest
	@GetMapping("/liste")
	public String utstillingListeHtmx(Model model) {
		model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
		model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
		return "utstilling/utstilling_fragments :: liste";
	}

	@GetMapping("/{id}")
	public String visUtstilling(Model model, @PathVariable("id") Long id, @AuthenticationPrincipal Bruker bruker) {
		logger.info("Viser utstilling med id: {}", id);
		Utstilling result = utstillingService.finnUtstillingMedId(id);
		if (result == null) {
			// TODO: handle error
			model.addAttribute("fragment", "liste");
			return "utstilling/utstilling";
		} else {
			if (bruker != null) {
				Paamelding paamelding = paameldingService.erBrukerPaameldtUtstilling(bruker, result);
				model.addAttribute("paamelding", paamelding);

				if (bruker.getRoller().contains(Rolle.ADMIN)) {
					List<Long> status = utstillingService.hentStatusPaaBedommelser(result);
					model.addAttribute("status", status);
				}
			}
			model.addAttribute("fragment", "detaljer");
			model.addAttribute("utstilling", result);
			return "utstilling/utstilling";
		}
	}

	@HxRequest
	@GetMapping("/{id}")
	public String visUtstillingHtmx(Model model, @PathVariable("id") Long id, @AuthenticationPrincipal Bruker bruker) {
		logger.info("Viser utstilling med id: {}", id);
		Utstilling result = utstillingService.finnUtstillingMedId(id);
		if (result == null) {
			// TODO: handle error
			return "utstilling/utstilling_fragments :: liste";
		} else {
			if (bruker != null) {
				Paamelding paamelding = paameldingService.erBrukerPaameldtUtstilling(bruker, result);
				model.addAttribute("paamelding", paamelding);

				if (bruker.getRoller().contains(Rolle.ADMIN)) {
					List<Long> status = utstillingService.hentStatusPaaBedommelser(result);
					model.addAttribute("status", status);
				}
			}
			model.addAttribute("utstilling", result);
			return "utstilling/utstilling_fragments :: detaljer";
		}
	}

	@HxRequest
	@GetMapping("/registrer")
	public View visRegistrerUtstillingHtmx(Model model) {
		model.addAttribute("utstilling", new Utstilling());
		return FragmentsRendering
				.with("utstilling/utstilling_fragments :: registreringsSkjema")
				.build();
	}

	@GetMapping("/registrer")
	public String visRegistrerUtstilling(Model model) {
		model.addAttribute("utstilling", new Utstilling());
		model.addAttribute("fragment", "registreringsSkjema");
		return "utstilling/utstilling";
	}


	@HxRequest
	@PostMapping("/registrer")
	public View opprettUtstillingHtmx(Model model, @ModelAttribute @Valid Utstilling utstilling,
	                                  BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			logger.info("Valideringsfeil: {}", bindingResult.getAllErrors());
			model.addAttribute("utstilling", utstilling);
			return FragmentsRendering
					.with("utstilling/utstilling_fragments :: registreringsSkjema")
					.build();
		}
		Utstilling saved = utstillingService.leggTilUtstilling(utstilling);
		model.addAttribute("utstilling", saved);
		return FragmentsRendering
				.with("utstilling/utstilling_fragments :: detaljer")
				.build();
	}

	@HxRequest
	@GetMapping("/rediger/{id}")
	public View visRedigerUtstillingHtmx(Model model, @PathVariable("id") Long id) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(id);
		if (utstilling == null) {
			logger.info("Fant ikke utstilling med id: {}", id);
			return FragmentsRendering
					.with("utstilling/utstilling_fragments :: liste")
					.build();
		}
		logger.info("Redigerer utstilling med id: {}", id);
		model.addAttribute("utstilling", utstilling);
		return FragmentsRendering
				.with("utstilling/utstilling_fragments :: redigerDetaljer")
				.build();
	}

	@HxRequest
	@PostMapping("/rediger/{id}")
	public View redigerUtstillingHtmx(Model model, @PathVariable("id") Long id,
	                                  @ModelAttribute @Valid Utstilling utstilling,
	                                  BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			logger.info("Valideringsfeil: {}", bindingResult.getAllErrors());
			model.addAttribute("utstilling", utstilling);
			return FragmentsRendering
					.with("utstilling/utstilling_fragments :: redigerDetaljer")
					.build();
		}
		Utstilling saved = utstillingService.oppdaterUtstilling(id, utstilling);
		if (saved == null) {
			return FragmentsRendering
					.with("utstilling/utstilling_fragments :: liste")
					.build();
		}
		model.addAttribute("utstilling", saved);
		return FragmentsRendering
				.with("utstilling/utstilling_fragments :: detaljer")
				.build();
	}

	@PreAuthorize("hasRole('ADMIN')")
	@HxRequest
	@PostMapping("/{id}/aapnePaamelding")
	public String aapnePaameldingHtmx(Model model, @PathVariable("id") Long id, @AuthenticationPrincipal Bruker bruker) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(id);
		if (utstilling == null) {
			logger.info("Fant ikke utstilling med id: {}", id);
			model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
			model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
			return "utstilling/utstilling_fragments :: liste";
		}
		logger.info("Åpner påmelding for utstilling med id: {}", id);
		utstilling.setPaameldingAApnet(true);
		Utstilling saved = utstillingService.oppdaterUtstilling(id, utstilling);
		if (saved == null) {
			model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
			model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
			return "utstilling/utstilling_fragments :: liste";
		}
		if (bruker != null) {
			Paamelding paamelding = paameldingService.erBrukerPaameldtUtstilling(bruker, saved);
			model.addAttribute("paamelding", paamelding);

			if (bruker.getRoller().contains(Rolle.ADMIN)) {
				List<Long> status = utstillingService.hentStatusPaaBedommelser(saved);
				model.addAttribute("status", status);
			}
		}
		model.addAttribute("utstilling", saved);
		return "utstilling/utstilling_fragments :: detaljer";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@HxRequest
	@PostMapping("/{id}/lukkPaamelding")
	public String lukkPaameldingHtmx(Model model, @PathVariable("id") Long id, @AuthenticationPrincipal Bruker bruker) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(id);
		if (utstilling == null) {
			logger.info("Fant ikke utstilling med id: {}", id);
			model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
			model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
			return "utstilling/utstilling_fragments :: liste";
		}
		logger.info("Lukker påmelding for utstilling med id: {}", id);
		utstilling.setPaameldingAApnet(false);
		Utstilling saved = utstillingService.oppdaterUtstilling(id, utstilling);
		if (saved == null) {
			model.addAttribute("kommendeUtstillinger", utstillingService.finnIkkeTidligereUtstillinger());
			model.addAttribute("tidligereUtstillinger", utstillingService.finnTidligereUtstillinger());
			return "utstilling/utstilling_fragments :: liste";
		}
		if (bruker != null) {
			Paamelding paamelding = paameldingService.erBrukerPaameldtUtstilling(bruker, saved);
			model.addAttribute("paamelding", paamelding);

			if (bruker.getRoller().contains(Rolle.ADMIN)) {
				List<Long> status = utstillingService.hentStatusPaaBedommelser(saved);
				model.addAttribute("status", status);
			}
		}
		model.addAttribute("utstilling", saved);
		return "utstilling/utstilling_fragments :: detaljer";
	}

	@GetMapping("/livescore/{utstillingId}")
	public String visLiveScore(@PathVariable("utstillingId") Long utstillingId, Model model) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		if (utstilling == null) {
			logger.info("Fant ikke utstilling med id: {}", utstillingId);
			return "utstilling/utstilling";
		}

		model.addAttribute("utstilling", utstilling);
		model.addAttribute("utstillingId", utstillingId);
		leggTilDuerIModel(utstillingId, model);

		return "utstilling/livescore";
	}

	@GetMapping("/livescore/getduer/{utstillingId}")
	public String oppdaterLiveScore(@PathVariable Long utstillingId, Model model) {
		model.addAttribute("utstillingId", utstillingId);
		leggTilDuerIModel(utstillingId, model);
		return "utstilling/livescore :: livescore-table";
	}

	private void leggTilDuerIModel(Long utstillingId, Model model) {
		try {
			List<Due> duer = utstillingService.finnAlleDuerFraUtstillingSomHarBedommelse(utstillingId);
			duer.sort((due1, due2) -> {
				LocalDateTime tid1 = due1.getBedommelse().getBedommelsesTidspunkt();
				LocalDateTime tid2 = due2.getBedommelse().getBedommelsesTidspunkt();

				if (tid1 == null && tid2 == null) {
					return 0;
				}
				if (tid1 == null) {
					return 1;
				}
				if (tid2 == null) {
					return -1;
				}

				return tid2.compareTo(tid1); // Sorterer slik at nyeste kommer først
			});
			model.addAttribute("duer", duer);
			logger.info("Hentet {} duer for utstilling med id {}", duer.size(), utstillingId);
		} catch (Exception e) {
			logger.error("Feil ved henting av duer for utstilling med id {}: {}", utstillingId, e.getMessage(), e);
			throw new RuntimeException("Kunne ikke hente duer for utstillingen", e);
		}
	}

	@ModelAttribute("utstillinger")
	public List<Utstilling> utstillinger() {
		return utstillingService.hentAlleUtstillinger();
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}
}
