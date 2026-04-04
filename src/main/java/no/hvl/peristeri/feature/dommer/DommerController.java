package no.hvl.peristeri.feature.dommer;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/dommer")
@RequiredArgsConstructor
public class DommerController {
	private static final String navLocation = "dommer";


	private final DommerService dommerService;

	@GetMapping
	public String dommer(@AuthenticationPrincipal Bruker bruker,
	                     Model model) {
		bruker = sikkerBruker(bruker);
		List<DommerPaamelding> dommerPaameldinger = dommerService.finnDommerPaameldinger(bruker);
		List<DommerPaamelding> historiskeDommerPaameldinger = finnHistoriskeDommerPaameldinger(dommerPaameldinger);
		List<DommerPaamelding> aktiveOgKommendeDommerPaameldinger = finnAktiveOgKommendeDommerPaameldinger(dommerPaameldinger);

		model.addAttribute("dommerPaameldinger", dommerPaameldinger);
		model.addAttribute("historiskeDommerPaameldinger", historiskeDommerPaameldinger);
		model.addAttribute("aktiveOgKommendeDommerPaameldinger", aktiveOgKommendeDommerPaameldinger);
		return "dommer/dommer";
	}

	@GetMapping("/utstilling/{utstillingId}")
	public String dommerUtstilling(@AuthenticationPrincipal Bruker bruker,
	                              @PathVariable Long utstillingId,
	                              Model model) {
		bruker = sikkerBruker(bruker);
		List<DommerPaamelding> dommereForValgtUtstilling = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId);

		model.addAttribute("valgtUtstillingId", utstillingId);
		model.addAttribute("duerPerRase", hentDuerPerRase(bruker, utstillingId, null));
		model.addAttribute("andreDommereMedDuer", hentAndreDommereMedDuer(bruker, utstillingId, dommereForValgtUtstilling));
		model.addAttribute("innloggetDommerId", bruker.getId());
		return "dommer/dommer_utstilling";
	}

	@HxRequest
	@GetMapping("/liste")
	public String dommerListeHtmx(@RequestParam Long utstillingId,
	                              @RequestParam(required = false) Integer filter,
	                              @AuthenticationPrincipal Bruker bruker,
	                              Model model) {
		bruker = sikkerBruker(bruker);
		model.addAttribute("valgtUtstillingId", utstillingId);
		model.addAttribute("duerPerRase", hentDuerPerRase(bruker, utstillingId, filter));
		return "dommer/dommer_fragments :: dueliste";
	}

	@HxRequest
	@GetMapping("/bedom")
	public String bedomDommerHtmx(@RequestParam Long dueId, @RequestParam Long utstillingId, Model model) {
		Due due = dommerService.hentDueMedId(dueId);

		Bedommelse bedommelse = due.getBedommelse();
		if(bedommelse == null) {
			bedommelse = new Bedommelse();
		}
		model.addAttribute("due", due);
		model.addAttribute("bedommelse", bedommelse);
		model.addAttribute("valgtUtstillingId", utstillingId);
		model.addAttribute("hovedkategorier", Arrays.asList(BedommingsKategori.values()));
		model.addAttribute("standardKommentarerPerKategori", standardKommentarerPerKategori());
		model.addAttribute("eksisterendeKategorier", eksisterendeKategorierPerKategori(bedommelse));
		model.addAttribute("eksisterendeStandardKommentarer", eksisterendeStandardKommentarerPerKategori(bedommelse));

		return "dommer/dommer_fragments :: dommerBedommelse";
	}

	@HxRequest
	@PostMapping("/bedom")
	public String lagreBedomming(@RequestParam Long dueId,
	                             @RequestParam Long utstillingId,
	                             @RequestParam MultiValueMap<String, String> skjemaData,
	                             @ModelAttribute Bedommelse bedommelse,
	                             Model model,
	                             @AuthenticationPrincipal Bruker bruker) {
		bruker = sikkerBruker(bruker);
		Map<BedommingsKategori, KategoriKommentar> kategorier = byggKategoriKommentarer(skjemaData);
		bedommelse.setKategorier(kategorier);
		bedommelse.setFordeler(oppsummerKategorierTilFordeler(kategorier));
		dommerService.lagreBedommelse(dueId, bedommelse, bruker, utstillingId);
		model.addAttribute("valgtUtstillingId", utstillingId);
		model.addAttribute("duerPerRase", hentDuerPerRase(bruker, utstillingId, null));
		return "dommer/dommer_fragments :: dueliste";
	}

	private List<DommerPaamelding> finnHistoriskeDommerPaameldinger(List<DommerPaamelding> dommerPaameldinger) {
		LocalDate iDag = LocalDate.now();
		return dommerPaameldinger.stream()
				.filter(dp -> dp.getUtstilling() != null
						&& dp.getUtstilling().getDatoRange() != null
						&& dp.getUtstilling().getDatoRange().getEndDate() != null
						&& dp.getUtstilling().getDatoRange().getEndDate().isBefore(iDag))
				.toList();
	}

	private List<DommerPaamelding> finnAktiveOgKommendeDommerPaameldinger(List<DommerPaamelding> dommerPaameldinger) {
		LocalDate iDag = LocalDate.now();
		return dommerPaameldinger.stream()
				.filter(dp -> dp.getUtstilling() != null
						&& dp.getUtstilling().getDatoRange() != null
						&& dp.getUtstilling().getDatoRange().getEndDate() != null
						&& !dp.getUtstilling().getDatoRange().getEndDate().isBefore(iDag))
				.toList();
	}

	private Map<DommerPaamelding, Map<String, List<Due>>> hentAndreDommereMedDuer(Bruker bruker,
	                                                                                Long utstillingId,
	                                                                                List<DommerPaamelding> dommereForValgtUtstilling) {
		return dommereForValgtUtstilling.stream()
				.filter(dp -> dp.getDommer() != null && !dp.getDommer().getId().equals(bruker.getId()))
				.collect(Collectors.toMap(
					dp -> dp,
					dp -> grupperDuerPerRase(dommerService.finnDuerDommerSkalBedomme(dp.getDommer(), utstillingId)),
					(a, b) -> a,
					LinkedHashMap::new
				));
	}


	private Map<String, List<String>> standardKommentarerPerKategori() {
		Map<String, List<String>> standarder = new LinkedHashMap<>();
		standarder.put(BedommingsKategori.HOLDNING.name(), List.of(
				"Bedre holdning",
				"Bedre balanse",
				"Bedre burtrening",
				"Bedre strekk",
				"Mer vannrett holdning",
				"Mer avfallende holdning"
		));
		standarder.put(BedommingsKategori.FIGUR.name(), List.of(
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
		standarder.put(BedommingsKategori.HODE.name(), List.of(
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
		standarder.put(BedommingsKategori.NEBB.name(), List.of(
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
		standarder.put(BedommingsKategori.OYNE.name(), List.of(
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
		standarder.put(BedommingsKategori.HALS_NAKKE.name(), List.of(
				"Smalere hals",
				"Kraftigere hals",
				"Lengre hals",
				"Kortere hals",
				"Mer innskåren strupe",
				"Bedre overgang hode/nakke",
				"Kraftigere nakke"
		));
		standarder.put(BedommingsKategori.VINGER.name(), List.of(
				"Kortere Vinger",
				"Lengre Vinger",
				"Bedre vingeføring",
				"Bedre samlede Vinger",
				"Bedre overgang vingeskjold til slagfjær",
				"Bedre vingelukning",
				"Bedre rygglukning"
		));
		standarder.put(BedommingsKategori.HALE.name(), List.of(
				"Kortere hale",
				"Lengre hale",
				"Smalere hale",
				"Mer samlet hale"
		));
		standarder.put(BedommingsKategori.BEIN.name(), List.of(
				"Kraftigere bein",
				"Kortere bein",
				"Lengre bein",
				"Bredere beinstilling",
				"Bedre strekk i bein",
				"Mer markerte lår",
				"Mindre markerte lår",
				"Rett neglfarge"
		));
		standarder.put(BedommingsKategori.FARGE.name(), List.of(
				"Bedre farge",
				"Lysere farge",
				"Jevnere farge",
				"Bedre grunnfarge",
				"Bedre skjold farge",
				"Mer glans i fargen",
				"Renere farge"
		));
		standarder.put(BedommingsKategori.TEGNING.name(), List.of(
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
		standarder.put(BedommingsKategori.FJAER_STRUKTUR.name(), List.of(
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

	private Map<String, KategoriKommentar> eksisterendeKategorierPerKategori(Bedommelse bedommelse) {
		Map<String, KategoriKommentar> eksisterende = new LinkedHashMap<>();
		if (bedommelse == null || bedommelse.getKategorier() == null) {
			return eksisterende;
		}
		for (Map.Entry<BedommingsKategori, KategoriKommentar> entry : bedommelse.getKategorier().entrySet()) {
			eksisterende.put(entry.getKey().name(), entry.getValue());
		}
		return eksisterende;
	}

	private Map<String, List<String>> eksisterendeStandardKommentarerPerKategori(Bedommelse bedommelse) {
		Map<String, List<String>> eksisterende = new LinkedHashMap<>();
		if (bedommelse == null || bedommelse.getKategorier() == null) {
			return eksisterende;
		}
		for (Map.Entry<BedommingsKategori, KategoriKommentar> entry : bedommelse.getKategorier().entrySet()) {
			eksisterende.put(entry.getKey().name(), splittStandardKommentarer(entry.getValue().getStandardKommentar()));
		}
		return eksisterende;
	}

	private Map<BedommingsKategori, KategoriKommentar> byggKategoriKommentarer(MultiValueMap<String, String> skjemaData) {
		Map<BedommingsKategori, KategoriKommentar> kategorier = new EnumMap<>(BedommingsKategori.class);
		for (BedommingsKategori kategori : BedommingsKategori.values()) {
			List<String> valgteStandarder = skjemaData.get("standardKommentar_" + kategori.name());
			String standard = joinStandardKommentarer(valgteStandarder);
			String fritekst = trimTilTomTekst(skjemaData.getFirst("fritekstKommentar_" + kategori.name()));
			if (!standard.isBlank() || !fritekst.isBlank()) {
				kategorier.put(kategori, new KategoriKommentar(standard, fritekst));
			}
		}
		return kategorier;
	}

	private String oppsummerKategorierTilFordeler(Map<BedommingsKategori, KategoriKommentar> kategorier) {
		return kategorier.entrySet().stream()
				.map(entry -> {
					KategoriKommentar kommentar = entry.getValue();
					String standard = String.join(", ", splittStandardKommentarer(kommentar.getStandardKommentar()));
					String fritekst = trimTilTomTekst(kommentar.getFritekstKommentar());
					if (standard.isBlank() && fritekst.isBlank()) {
						return "";
					}
					if (standard.isBlank()) {
						return entry.getKey().getVisningsnavn() + ": " + fritekst;
					}
					if (fritekst.isBlank()) {
						return entry.getKey().getVisningsnavn() + ": " + standard;
					}
					return entry.getKey().getVisningsnavn() + ": " + standard + " / " + fritekst;
				})
				.filter(tekst -> !tekst.isBlank())
				.collect(Collectors.joining("\n"));
	}

	private String trimTilTomTekst(String tekst) {
		return tekst == null ? "" : tekst.trim();
	}

	private String joinStandardKommentarer(List<String> kommentarer) {
		if (kommentarer == null) {
			return "";
		}
		return kommentarer.stream()
				.map(this::trimTilTomTekst)
				.filter(tekst -> !tekst.isBlank())
				.distinct()
				.collect(Collectors.joining("\n"));
	}

	private List<String> splittStandardKommentarer(String lagret) {
		String tekst = trimTilTomTekst(lagret);
		if (tekst.isBlank()) {
			return List.of();
		}
		return Arrays.stream(tekst.split("\\n"))
				.map(this::trimTilTomTekst)
				.filter(verdi -> !verdi.isBlank())
				.toList();
	}

	private Bruker sikkerBruker(Bruker bruker) {
		if (bruker != null) {
			return bruker;
		}
		Bruker fallback = new Bruker();
		fallback.setId(-1L);
		return fallback;
	}

	private Map<String, List<Due>> hentDuerPerRase(Bruker bruker, Long utstillingId, Integer filter) {
		if (utstillingId == null) {
			return Map.of();
		}

		List<Due> duer;
		if (filter == null) {
			duer = dommerService.finnDuerDommerSkalBedomme(bruker, utstillingId);
		} else {
			duer = List.of(dommerService.finnDueDommerSkalBedommeMedBurnummer(bruker, filter, utstillingId));
		}

		return grupperDuerPerRase(duer);
	}

	private Map<String, List<Due>> grupperDuerPerRase(List<Due> duer) {
		return duer.stream().collect(Collectors.groupingBy(
				due -> due.getRase() == null || due.getRase().isBlank() ? "Ukjent rase" : due.getRase(),
				LinkedHashMap::new,
				Collectors.toList()
		));
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

}
