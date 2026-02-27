package no.hvl.peristeri.feature.paamelding;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.BusinessRuleViolationException;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.DueService;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/paamelding")
public class PaameldingController {
	private static final String navLocation = "paamelding";

	private final Logger logger = LoggerFactory.getLogger(PaameldingController.class);

	private final PaameldingService paameldingService;
	private final DueService        dueService;
	private final UtstillingService utstillingService;

	@GetMapping
	public String paamelding(@AuthenticationPrincipal Bruker bruker, Model model, HttpSession session) {
		model.addAttribute("utstiller", bruker);
		List<Utstilling> utstillinger = utstillingService.finnUtstillingerMedMulighetForPaamelding();
		model.addAttribute("utstillinger", utstillinger);
		return "paamelding/paamelding";
	}

	@PostMapping("/paameldingskvittering")
	public String utstillerKvittering(Model model, @AuthenticationPrincipal Bruker bruker, HttpSession session,
	                                  Long utstillingId, RedirectAttributes ra) {
		model.addAttribute("utstiller", bruker);

		if (utstillingId == null) {
			model.addAttribute("ingenUtstilling", "Du må velge en utstilling før du kan melde deg på.");
			List<Utstilling> utstillinger = utstillingService.finnUtstillingerMedMulighetForPaamelding();
			model.addAttribute("utstillinger", utstillinger);
			return "paamelding/paamelding";
		}

		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);

		if (paameldingService.sjekkOmBrukerAlleredeErPaameldt(bruker, utstilling)) {
			throw new BusinessRuleViolationException("Du er allerede påmeldt til denne utstillingen.");
		}

		session.setAttribute("utstilling", utstilling);
		session.setAttribute("dueDTOListe", new DueDTOList());

		logger.info("Påmelding startet for utstilling: {} ({})", utstilling.getTittel(), utstillingId);
		model.addAttribute("radId", 1);

		return "paamelding/paameldingkvittering :: kvittering";
	}

	@PostMapping("/duekvittering")
	public String duekvittering(@ModelAttribute DueDTO dueDTO, Model model, HttpSession session) {
		logger.info("PaameldingDTO: {}", dueDTO);

		if (dueDTO.hunnerUng() == 0 && dueDTO.hunnerEldre() == 0 && dueDTO.hannerUng() == 0 &&
		    dueDTO.hannerEldre() == 0) {
			model.addAttribute("errorMessage", "Du må melde på minst én due.");
			return "paamelding/due_tabell :: due-tabell";
		}
		DueDTOList duerDTO = getDueList(session);

		duerDTO.leggTilDueDTO(dueDTO);

		Integer antallDuer = paameldingService.antallDuer(getDueList(session).getListe());
		model.addAttribute("antallDuer", antallDuer);

		return "paamelding/due_tabell :: due-tabell";
	}

	@GetMapping("/redigerDue/{radId}")
	public String redigerDueRad(Model model, @PathVariable Integer radId, HttpSession session) {
		DueDTO dueRad = getDueList(session).findDueById(radId);
		logger.info("Duerad: {}", dueRad);
		model.addAttribute("dueDTO", dueRad);

		return "paamelding/due_tabell :: redigerDueRad";
	}

	@PostMapping("/oppdaterDue/{radId}")
	public String oppdaterDueRad(@ModelAttribute DueDTO due, Model model, @PathVariable Integer radId,
	                             HttpSession session) {
		model.addAttribute("due", due);
		getDueList(session).endreDue(due);
		logger.info("Lagre knapp trykket: {}", due);

		return "paamelding/due_tabell :: oppdatertDueRad";
	}

	@PostMapping("/submitPaamelding")
	public String submitPaamelding(@AuthenticationPrincipal Bruker bruker, Model model, HttpSession session) {
		Integer antallDuer = paameldingService.antallDuer(getDueList(session).getListe());
		model.addAttribute("antallDuer", antallDuer);

		BigDecimal totalPris = paameldingService.beregnTotalPris(antallDuer,
				((Utstilling) session.getAttribute("utstilling")).getDuePris());
		session.setAttribute("totalPris", totalPris);

		paameldingService.leggTilPaamelding(bruker.getId(), ((Utstilling) session.getAttribute("utstilling")).getId(),
				getDueList(session), (BigDecimal) session.getAttribute("totalPris"));

		logger.info("Utstiller: {}, Utstilling: {}, DueDTOList: {}", bruker, session.getAttribute("utstilling"),
				getDueList(session));
		model.addAttribute("utstiller", bruker);
		return "paamelding/paameldt :: paameldt";
	}

	@GetMapping("/{utstillingId}")
	public String meldPaaMedValgtUtstillingFraFoer(@AuthenticationPrincipal Bruker bruker, @PathVariable Long utstillingId, Model model) {
		model.addAttribute("forhandsvalgtUtstillingId", utstillingId);
		model.addAttribute("utstiller", bruker);
		List<Utstilling> utstillinger = utstillingService.finnUtstillingerMedMulighetForPaamelding();
		model.addAttribute("utstillinger", utstillinger);
		return "paamelding/paamelding";
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

	public DueDTOList getDueList(HttpSession session) {
		DueDTOList duerDTO = (DueDTOList) session.getAttribute("dueDTOListe");
		if (duerDTO == null) {
			duerDTO = new DueDTOList();
			session.setAttribute("dueDTOListe", duerDTO);
		}
		return duerDTO;
	}
}
