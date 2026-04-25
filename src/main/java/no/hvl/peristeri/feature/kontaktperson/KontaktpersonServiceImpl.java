package no.hvl.peristeri.feature.kontaktperson;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KontaktpersonServiceImpl implements KontaktpersonService {

    private final KontaktpersonRepository kontaktpersonRepository;

    @Override
    public List<Kontaktperson> hentAlle() {
        return kontaktpersonRepository.findAllByOrderByRolleAscNavnAsc();
    }

    @Override
    @Transactional
    public Kontaktperson opprett(Kontaktperson kontaktperson) {
        normaliser(kontaktperson);
        return kontaktpersonRepository.save(kontaktperson);
    }

    @Override
    @Transactional
    public Kontaktperson oppdater(Long id, Kontaktperson kontaktperson) {
        Kontaktperson eksisterende = kontaktpersonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kontaktperson", id));

        eksisterende.setRolle(kontaktperson.getRolle());
        eksisterende.setNavn(kontaktperson.getNavn());
        eksisterende.setTelefon(kontaktperson.getTelefon());
        eksisterende.setEpost(kontaktperson.getEpost());
        normaliser(eksisterende);

        return kontaktpersonRepository.save(eksisterende);
    }

    @Override
    @Transactional
    public void slett(Long id) {
        if (!kontaktpersonRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kontaktperson", id);
        }
        kontaktpersonRepository.deleteById(id);
    }

    private void normaliser(Kontaktperson kontaktperson) {
        kontaktperson.setRolle(trimEllerNull(kontaktperson.getRolle()));
        kontaktperson.setNavn(trimEllerNull(kontaktperson.getNavn()));
        kontaktperson.setTelefon(trimEllerNull(kontaktperson.getTelefon()));
        kontaktperson.setEpost(trimEllerNull(kontaktperson.getEpost()));
    }

    private String trimEllerNull(String verdi) {
        if (verdi == null) {
            return null;
        }

        String trimmet = verdi.trim();
        return trimmet.isEmpty() ? null : trimmet;
    }
}

