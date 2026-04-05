package no.hvl.peristeri.feature.dommer;

import java.util.List;
import java.util.Map;

public interface StandardKommentarService {
	Map<String, List<String>> hentKommentarTeksterPerKategori(StandardKommentarType type);

	Map<String, List<StandardKommentar>> hentKommentarerPerKategori(StandardKommentarType type);

	void leggTilKommentar(BedommingsKategori kategori, StandardKommentarType type, String tekst);

	void slettKommentar(Long id);
}

