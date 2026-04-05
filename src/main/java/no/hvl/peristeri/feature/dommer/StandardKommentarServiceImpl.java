package no.hvl.peristeri.feature.dommer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StandardKommentarServiceImpl implements StandardKommentarService {
	private final StandardKommentarRepository repository;

	@PostConstruct
	public void initDefaultsIfEmpty() {
		hentStandardDefaults().forEach((kategori, kommentarer) ->
				kommentarer.stream().distinct().forEach(tekst -> {
					leggTilKommentar(kategori, StandardKommentarType.STANDARD, tekst);
					leggTilKommentar(kategori, StandardKommentarType.ONSKER, tekst);
				}));
		hentFeilDefaults().forEach((kategori, kommentarer) ->
				kommentarer.stream().distinct().forEach(tekst -> leggTilKommentar(kategori, StandardKommentarType.FEIL, tekst)));
	}

	@Override
	public Map<String, List<String>> hentKommentarTeksterPerKategori(StandardKommentarType type) {
		if (type == null) {
			throw new InvalidParameterException("type", "cannot be null");
		}

		Map<String, List<String>> resultat = tomKategoriMapTekst();
		repository.findByTypeOrderByKategoriAscIdAsc(type).forEach(kommentar ->
				resultat.get(kommentar.getKategori().name()).add(kommentar.getTekst()));
		return resultat;
	}

	@Override
	public Map<String, List<StandardKommentar>> hentKommentarerPerKategori(StandardKommentarType type) {
		if (type == null) {
			throw new InvalidParameterException("type", "cannot be null");
		}

		Map<String, List<StandardKommentar>> resultat = tomKategoriMapObjekter();
		repository.findByTypeOrderByKategoriAscIdAsc(type).forEach(kommentar ->
				resultat.get(kommentar.getKategori().name()).add(kommentar));
		return resultat;
	}

	@Override
	public void leggTilKommentar(BedommingsKategori kategori, StandardKommentarType type, String tekst) {
		if (kategori == null) {
			throw new InvalidParameterException("kategori", "cannot be null");
		}
		if (type == null) {
			throw new InvalidParameterException("type", "cannot be null");
		}
		if (tekst == null) {
			throw new InvalidParameterException("tekst", "cannot be null");
		}

		String trimmet = tekst.trim();
		if (trimmet.isBlank()) {
			return;
		}
		if (repository.existsByTypeAndKategoriAndTekstIgnoreCase(type, kategori, trimmet)) {
			return;
		}
		repository.save(new StandardKommentar(kategori, type, trimmet));
	}

	@Override
	public void slettKommentar(Long id) {
		if (id == null) {
			throw new InvalidParameterException("id", "cannot be null");
		}
		repository.deleteById(id);
	}

	private Map<BedommingsKategori, List<String>> hentStandardDefaults() {
		Map<BedommingsKategori, List<String>> standarder = new EnumMap<>(BedommingsKategori.class);
		standarder.put(BedommingsKategori.HOLDNING, List.of(
				"Bedre holdning",
				"Bedre balanse",
				"Bedre burtrening",
				"Bedre strekk",
				"Mer vannrett holdning",
				"Mer avfallende holdning"
		));
		standarder.put(BedommingsKategori.FIGUR, List.of(
				"Kraftigere figur",
				"Kortere figur",
				"Lengre figur",
				"Smalere figur",
				"Bredere bryst",
				"Mer brystfylde",
				"Mer brystfylde",
				"Bredere bakkropp",
				"Bedre avrunding bak bein"
		));
		standarder.put(BedommingsKategori.HODE, List.of(
				"Bredere panne",
				"Mer markant panne",
				"Mer panne fylde",
				"Mer avrundet hode",
				"Mer høyde over øyet",
				"Bedre overgang til nakke",
				"Mindre bakhode",
				"Større hode",
				"Mer markerte kinn"
		));
		standarder.put(BedommingsKategori.NEBB, List.of(
				"Kraftigere nebb",
				"Kraftigere under nebb",
				"Bedre nebbvinkel",
				"Bedre nebb farge",
				"Finere nebbvorter",
				"Bedre pusset nebbvorter",
				"Mer markerte nebbvorter",
				"Kortere nebb",
				"Lengre nebb"
		));
		standarder.put(BedommingsKategori.OYNE, List.of(
				"Renere iris",
				"Bedre øyenfarge",
				"Finere øyenrand",
				"Rundere øyenrand",
				"Mer utbygd øyenrand",
				"Rødere øyenrand",
				"Mørkere øyenrand",
				"Lysere øyenrand",
				"Smalere øyenrand"
		));
		standarder.put(BedommingsKategori.HALS_NAKKE, List.of(
				"Smalere hals",
				"Kraftigere hals",
				"Lengre hals",
				"Kortere hals",
				"Mer innskåren strupe",
				"Bedre overgang hode/nakke",
				"Kraftigere nakke"
		));
		standarder.put(BedommingsKategori.VINGER, List.of(
				"Kortere Vinger",
				"Lengre Vinger",
				"Bedre vingeføring",
				"Bedre samlede Vinger",
				"Bedre overgang vingeskjold til slagfjær",
				"Bedre vingelukning",
				"Bedre rygglukning"
		));
		standarder.put(BedommingsKategori.HALE, List.of(
				"Kortere hale",
				"Lengre hale",
				"Smalere hale",
				"Mer samlet hale"
		));
		standarder.put(BedommingsKategori.BEIN, List.of(
				"Kraftigere bein",
				"Kortere bein",
				"Lengre bein",
				"Bredere beinstilling",
				"Bedre strekk i bein",
				"Mer markerte lår",
				"Mindre markerte lår",
				"Rett neglfarge"
		));
		standarder.put(BedommingsKategori.FARGE, List.of(
				"Bedre farge",
				"Lysere farge",
				"Jevnere farge",
				"Bedre grunnfarge",
				"Bedre skjold farge",
				"Mer glans i fargen",
				"Renere farge"
		));
		standarder.put(BedommingsKategori.TEGNING, List.of(
				"Bedre tegning",
				"Jevnere tegning",
				"Åpnere tegning",
				"Tettere tegning",
				"Smalere bånd",
				"Jevnere bånd",
				"Lengre bånd",
				"Renere bånd",
				"Bedre brekning i tegning"
		));
		standarder.put(BedommingsKategori.FJAER_STRUKTUR, List.of(
				"Fyldigere krone",
				"Fastere krone",
				"Fastere nakke",
				"Mer symmetriske rosetter",
				"Jevnere rosetter",
				"Tydligere rosetter",
				"Fyldigere sokker",
				"Bedre inner sokker",
				"Jevnere sokker",
				"Bedre gribbfjær",
				"Fyldigere kappe",
				"Mer oval nebbrosett",
				"Rundere nebbrosett",
				"Bedre kryss",
				"Bedre frisering",
				"Jevnere parykk"
		));
		return standarder;
	}

	private Map<BedommingsKategori, List<String>> hentFeilDefaults() {
		Map<BedommingsKategori, List<String>> feil = new EnumMap<>(BedommingsKategori.class);
		for (BedommingsKategori kategori : BedommingsKategori.values()) {
			feil.put(kategori, List.of());
		}
		feil.put(BedommingsKategori.HOLDNING, List.of("Utypisk", "For høyreist stilling", "For lavstilt stilling"));
		feil.put(BedommingsKategori.FIGUR, List.of("For lang"));
		feil.put(BedommingsKategori.OYNE, List.of("Feil øyefarge"));
		feil.put(BedommingsKategori.HALE, List.of("11 halefjær", "13 halefjær"));
		feil.put(BedommingsKategori.BEIN, List.of("Mangler sokker"));
		feil.put(BedommingsKategori.FARGE, List.of("Feil farge"));
		feil.put(BedommingsKategori.TEGNING, List.of("Feil tegning"));
		feil.put(BedommingsKategori.FJAER_STRUKTUR, List.of("Feil i fjærdrakt", "Mangel i krone", "Mangler rosetter", "Utøy"));
		return feil;
	}

	private Map<String, List<String>> tomKategoriMapTekst() {
		Map<String, List<String>> map = new LinkedHashMap<>();
		for (BedommingsKategori kategori : BedommingsKategori.values()) {
			map.put(kategori.name(), new ArrayList<>());
		}
		return map;
	}

	private Map<String, List<StandardKommentar>> tomKategoriMapObjekter() {
		Map<String, List<StandardKommentar>> map = new LinkedHashMap<>();
		for (BedommingsKategori kategori : BedommingsKategori.values()) {
			map.put(kategori.name(), new ArrayList<>());
		}
		return map;
	}
}

