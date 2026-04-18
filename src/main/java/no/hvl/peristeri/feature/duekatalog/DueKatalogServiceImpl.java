package no.hvl.peristeri.feature.duekatalog;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.BusinessRuleViolationException;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DueKatalogServiceImpl implements DueKatalogService {

    private final DueGruppeRepository dueGruppeRepository;
    private final DueRaseRepository dueRaseRepository;
    private final DueFargeRepository dueFargeRepository;
    private final DueVariantRepository dueVariantRepository;

    @Override
    public List<DueGruppe> finnAlleGrupper() {
        return dueGruppeRepository.findAllByOrderByNavnAsc();
    }

    @Override
    public List<DueRase> finnAlleRaser() {
        return dueRaseRepository.findAllByOrderByNavnAsc();
    }

    @Override
    public List<DueRase> finnRaserForGruppe(Long gruppeId) {
        if (gruppeId == null) {
            return List.of();
        }
        return dueRaseRepository.findByGruppeIdOrderByNavnAsc(gruppeId);
    }

    @Override
    public List<DueFarge> finnAlleFarger() {
        return dueFargeRepository.findAllByOrderByNavnAsc();
    }

    @Override
    public List<DueVariant> finnAlleVarianter() {
        return dueVariantRepository.findAllByOrderByNavnAsc();
    }

    @Transactional
    @Override
    public DueGruppe opprettGruppe(String navn) {
        String normalisertNavn = normaliserNavn(navn, "gruppenavn");
        if (dueGruppeRepository.existsByNavnIgnoreCase(normalisertNavn)) {
            throw new BusinessRuleViolationException("Gruppen finnes allerede.");
        }
        return dueGruppeRepository.save(new DueGruppe(normalisertNavn));
    }

    @Transactional
    @Override
    public DueRase opprettRase(Long gruppeId, String navn) {
        if (gruppeId == null) {
            throw new InvalidParameterException("gruppeId", "cannot be null");
        }
        String normalisertNavn = normaliserNavn(navn, "rasenavn");
        if (dueRaseRepository.existsByGruppeIdAndNavnIgnoreCase(gruppeId, normalisertNavn)) {
            throw new BusinessRuleViolationException("Rasen finnes allerede i denne gruppen.");
        }
        DueGruppe gruppe = dueGruppeRepository.findById(gruppeId)
                .orElseThrow(() -> new ResourceNotFoundException("DueGruppe", gruppeId));
        return dueRaseRepository.save(new DueRase(normalisertNavn, gruppe));
    }

    @Transactional
    @Override
    public DueFarge opprettFarge(String navn) {
        String normalisertNavn = normaliserNavn(navn, "farge");
        if (dueFargeRepository.existsByNavnIgnoreCase(normalisertNavn)) {
            throw new BusinessRuleViolationException("Fargen finnes allerede.");
        }
        return dueFargeRepository.save(new DueFarge(normalisertNavn));
    }

    @Transactional
    @Override
    public DueVariant opprettVariant(String navn) {
        String normalisertNavn = normaliserNavn(navn, "variant");
        if (dueVariantRepository.existsByNavnIgnoreCase(normalisertNavn)) {
            throw new BusinessRuleViolationException("Varianten finnes allerede.");
        }
        return dueVariantRepository.save(new DueVariant(normalisertNavn));
    }

    @Transactional
    @Override
    public void slettGruppe(Long gruppeId) {
        if (gruppeId == null) {
            throw new InvalidParameterException("gruppeId", "cannot be null");
        }
        if (!dueGruppeRepository.existsById(gruppeId)) {
            throw new ResourceNotFoundException("DueGruppe", gruppeId);
        }
        if (dueRaseRepository.countByGruppeId(gruppeId) > 0) {
            throw new BusinessRuleViolationException("Kan ikke slette en gruppe som har raser.");
        }
        dueGruppeRepository.deleteById(gruppeId);
    }

    @Transactional
    @Override
    public void slettRase(Long raseId) {
        slettHvisFinnes(raseId, "DueRase", dueRaseRepository::existsById, dueRaseRepository::deleteById);
    }

    @Transactional
    @Override
    public void slettFarge(Long fargeId) {
        slettHvisFinnes(fargeId, "DueFarge", dueFargeRepository::existsById, dueFargeRepository::deleteById);
    }

    @Transactional
    @Override
    public void slettVariant(Long variantId) {
        slettHvisFinnes(variantId, "DueVariant", dueVariantRepository::existsById, dueVariantRepository::deleteById);
    }

    @Override
    public boolean erRaseGyldigForGruppe(Long gruppeId, String rase) {
        if (gruppeId == null || rase == null || rase.isBlank()) {
            return false;
        }
        return dueRaseRepository.findByGruppeIdAndNavnIgnoreCase(gruppeId, rase.trim()).isPresent();
    }

    @Override
    public boolean finnesFarge(String farge) {
        return farge != null && !farge.isBlank() && dueFargeRepository.existsByNavnIgnoreCase(farge.trim());
    }

    @Override
    public boolean finnesVariant(String variant) {
        return variant != null && !variant.isBlank() && dueVariantRepository.existsByNavnIgnoreCase(variant.trim());
    }

    private String normaliserNavn(String navn, String felt) {
        if (navn == null || navn.isBlank()) {
            throw new InvalidParameterException(felt, "cannot be blank");
        }
        return navn.trim();
    }

    private void slettHvisFinnes(Long id,
                                 String resourceNavn,
                                 java.util.function.Predicate<Long> exists,
                                 java.util.function.Consumer<Long> delete) {
        if (id == null) {
            throw new InvalidParameterException("id", "cannot be null");
        }
        if (!exists.test(id)) {
            throw new ResourceNotFoundException(resourceNavn, id);
        }
        delete.accept(id);
    }
}

