package no.hvl.peristeri.feature.admin;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import no.hvl.peristeri.feature.bruker.Rolle;
import no.hvl.peristeri.feature.dommer.DommerService;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/dommere")
@RequiredArgsConstructor
public class AdminDommerApiController {

	private final DommerService dommerService;
	private final BrukerService brukerService;

	@GetMapping
	public List<DommerDto> hentAlleDommere() {
		return dommerService.hentDommere().stream()
				.map(this::tilDommerDto)
				.toList();
	}

	@PostMapping("/{brukerId}")
	public ResponseEntity<DommerDto> leggTilDommerrolle(@PathVariable Long brukerId,
	                                                    @RequestBody(required = false) TildelUtstillingerRequest request) {
		if (brukerId == null) {
			throw new InvalidParameterException("brukerId", "cannot be null");
		}

		dommerService.tildelDommerRolle(brukerId);
		if (request != null && request.utstillingIder() != null) {
			dommerService.tildelDommerTilUtstillinger(brukerId, request.utstillingIder());
		}

		Bruker oppdatert = brukerService.hentBrukerMedId(brukerId);
		return ResponseEntity.status(HttpStatus.CREATED).body(tilDommerDto(oppdatert));
	}

	@PostMapping("/{brukerId}/utstillinger")
	public DommerDto tildelUtstillingerTilDommer(@PathVariable Long brukerId,
	                                             @RequestBody TildelUtstillingerRequest request) {
		if (brukerId == null) {
			throw new InvalidParameterException("brukerId", "cannot be null");
		}
		if (request == null || request.utstillingIder() == null) {
			throw new InvalidParameterException("utstillingIder", "cannot be null");
		}
		if (!brukerService.harRolle(brukerId, Rolle.DOMMER)) {
			throw new InvalidParameterException("brukerId", "bruker har ikke dommerrolle");
		}

		dommerService.tildelDommerTilUtstillinger(brukerId, request.utstillingIder());
		return tilDommerDto(brukerService.hentBrukerMedId(brukerId));
	}

	@DeleteMapping("/{brukerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void fjernDommerrolle(@PathVariable Long brukerId) {
		dommerService.fjernDommerRolle(brukerId);
	}

	private DommerDto tilDommerDto(Bruker dommer) {
		List<Long> utstillingIder = dommerService.hentUtstillingerForDommer(dommer.getId()).stream()
				.map(Utstilling::getId)
				.toList();
		return new DommerDto(
				dommer.getId(),
				dommer.getFornavn(),
				dommer.getEtternavn(),
				dommer.getEpost(),
				utstillingIder
		);
	}

	public record TildelUtstillingerRequest(List<Long> utstillingIder) {
	}

	public record DommerDto(Long brukerId, String fornavn, String etternavn, String epost, List<Long> utstillingIder) {
	}
}

