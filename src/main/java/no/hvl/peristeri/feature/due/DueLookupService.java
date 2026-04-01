package no.hvl.peristeri.feature.due;

import java.util.List;

public interface DueLookupService {
	List<Rase> hentAlleRaser();

	List<Farge> hentAlleFarger();

	List<Variant> hentAlleVarianter();

	Rase finnRaseMedId(Long id);

	Farge finnFargeMedId(Long id);

	Variant finnVariantMedId(Long id);

	Rase finnEllerLagRase(String navn, String gruppe);

	Farge finnEllerLagFarge(String navn);

	Variant finnEllerLagVariant(String navn);
}

