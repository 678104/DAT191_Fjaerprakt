package no.hvl.peristeri.feature.duekatalog;

import java.util.List;

public interface DueKatalogService {

    List<DueGruppe> finnAlleGrupper();

    List<DueRase> finnAlleRaser();

    List<DueRase> finnRaserForGruppe(Long gruppeId);

    List<DueFarge> finnAlleFarger();

    List<DueVariant> finnAlleVarianter();

    DueGruppe opprettGruppe(String navn);

    DueRase opprettRase(Long gruppeId, String navn);

    DueFarge opprettFarge(String navn);

    DueVariant opprettVariant(String navn);

    void slettGruppe(Long gruppeId);

    void slettRase(Long raseId);

    void slettFarge(Long fargeId);

    void slettVariant(Long variantId);

    boolean erRaseGyldigForGruppe(Long gruppeId, String rase);

    boolean finnesFarge(String farge);

    boolean finnesVariant(String variant);
}

