package no.hvl.peristeri.feature.kontaktperson;

import java.util.List;

public interface KontaktpersonService {

    List<Kontaktperson> hentAlle();

    Kontaktperson opprett(Kontaktperson kontaktperson);

    Kontaktperson oppdater(Long id, Kontaktperson kontaktperson);

    void slett(Long id);
}

