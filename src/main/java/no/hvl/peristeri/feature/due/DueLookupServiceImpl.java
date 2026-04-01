package no.hvl.peristeri.feature.due;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class DueLookupServiceImpl implements DueLookupService {

	private final RaseRepository raseRepository;
	private final FargeRepository fargeRepository;
	private final VariantRepository variantRepository;

	@Override
	public List<Rase> hentAlleRaser() {
		return raseRepository.findAllByOrderByGruppeAscNavnAsc();
	}

	@Override
	public List<Farge> hentAlleFarger() {
		return fargeRepository.findAllByOrderByNavnAsc();
	}

	@Override
	public List<Variant> hentAlleVarianter() {
		return variantRepository.findAllByOrderByNavnAsc();
	}

	@Override
	public Rase finnRaseMedId(Long id) {
		return raseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rase", id));
	}

	@Override
	public Farge finnFargeMedId(Long id) {
		return fargeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Farge", id));
	}

	@Override
	public Variant finnVariantMedId(Long id) {
		return variantRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Variant", id));
	}

	@Override
	public Rase finnEllerLagRase(String navn, String gruppe) {
		if (navn == null || navn.isBlank()) {
			return null;
		}
		String groupValue = (gruppe == null || gruppe.isBlank()) ? "Ukjent" : gruppe.trim();
		return raseRepository.findByNavnIgnoreCaseAndGruppeIgnoreCase(navn.trim(), groupValue)
		                     .orElseGet(() -> raseRepository.save(new Rase(navn.trim(), groupValue)));
	}

	@Override
	public Farge finnEllerLagFarge(String navn) {
		if (navn == null || navn.isBlank()) {
			return null;
		}
		return fargeRepository.findByNavnIgnoreCase(navn.trim())
		                      .orElseGet(() -> fargeRepository.save(new Farge(navn.trim())));
	}

	@Override
	public Variant finnEllerLagVariant(String navn) {
		if (navn == null || navn.isBlank()) {
			return null;
		}
		return variantRepository.findByNavnIgnoreCase(navn.trim())
		                        .orElseGet(() -> variantRepository.save(new Variant(navn.trim())));
	}
}

