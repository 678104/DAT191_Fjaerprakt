package no.hvl.peristeri.feature.dommer;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
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
	private static final long MAKS_BILDESTORRELSE_BYTES = 5L * 1024 * 1024;


	private final DommerService dommerService;
	private final StandardKommentarService standardKommentarService;

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
		leggTilVinnerData(model, bruker, utstillingId);
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
		leggTilVinnerData(model, bruker, utstillingId);
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
		Map<String, List<String>> standardFordelerKommentarer = standardFordelerKommentarerPerKategori();
		Map<String, List<String>> standardOnskerKommentarer = standardOnskerKommentarerPerKategori();
		Map<String, List<String>> standardFeilKommentarer = standardFeilKommentarerPerKategori();
		Map<String, KategoriKommentar> eksisterendeOnskerKategorier = eksisterendeKategorierFraTekst(bedommelse.getOnsker(), standardOnskerKommentarer);
		Map<String, KategoriKommentar> eksisterendeFeilKategorier = eksisterendeKategorierFraTekst(bedommelse.getFeil(), standardFeilKommentarer);
		model.addAttribute("due", due);
		model.addAttribute("bedommelse", bedommelse);
		model.addAttribute("valgtUtstillingId", utstillingId);
		model.addAttribute("hovedkategorier", Arrays.asList(BedommingsKategori.values()));
		model.addAttribute("standardKommentarerPerKategori", standardFordelerKommentarer);
		model.addAttribute("standardOnskerKommentarerPerKategori", standardOnskerKommentarer);
		model.addAttribute("standardFeilKommentarerPerKategori", standardFeilKommentarer);
		model.addAttribute("eksisterendeFordelerKategorier", eksisterendeKategorierPerKategori(bedommelse));
		model.addAttribute("eksisterendeFordelerStandardKommentarer", eksisterendeStandardKommentarerPerKategori(bedommelse));
		model.addAttribute("eksisterendeOnskerKategorier", eksisterendeOnskerKategorier);
		model.addAttribute("eksisterendeOnskerStandardKommentarer", eksisterendeStandardKommentarerPerKategori(eksisterendeOnskerKategorier));
		model.addAttribute("eksisterendeFeilKategorier", eksisterendeFeilKategorier);
		model.addAttribute("eksisterendeFeilStandardKommentarer", eksisterendeStandardKommentarerPerKategori(eksisterendeFeilKategorier));
		model.addAttribute("harBilde", bedommelse.getBilde() != null);

		return "dommer/dommer_fragments :: dommerBedommelse";
	}

	@HxRequest
	@PostMapping("/bedom")
	public String lagreBedomming(@RequestParam Long dueId,
	                             @RequestParam Long utstillingId,
	                             @RequestParam(name = "bildeFil", required = false) MultipartFile bilde,
	                             @RequestParam MultiValueMap<String, String> skjemaData,
	                             @ModelAttribute Bedommelse bedommelse,
	                             Model model,
	                             @AuthenticationPrincipal Bruker bruker) {
		bruker = sikkerBruker(bruker);
		Map<BedommingsKategori, KategoriKommentar> fordeler = byggKategoriKommentarer(skjemaData, "standardKommentarFordeler_", "fritekstKommentarFordeler_");
		Map<BedommingsKategori, KategoriKommentar> onsker = byggKategoriKommentarer(skjemaData, "standardKommentarOnsker_", "fritekstKommentarOnsker_");
		Map<BedommingsKategori, KategoriKommentar> feil = byggKategoriKommentarer(skjemaData, "standardKommentarFeil_", "fritekstKommentarFeil_");
		bedommelse.setKategorier(fordeler);
		bedommelse.setFordeler(oppsummerKategorierTilTekst(fordeler));
		bedommelse.setOnsker(oppsummerKategorierTilTekst(onsker));
		bedommelse.setFeil(oppsummerKategorierTilTekst(feil));
		bedommelse.setBilde(opprettBilde(bilde));
		dommerService.lagreBedommelse(dueId, bedommelse, bruker, utstillingId);
		model.addAttribute("valgtUtstillingId", utstillingId);
		model.addAttribute("duerPerRase", hentDuerPerRase(bruker, utstillingId, null));
		leggTilVinnerData(model, bruker, utstillingId);
		return "dommer/dommer_fragments :: dueliste";
	}

	@HxRequest
	@PostMapping("/vinnere")
	public String lagreVinnere(@RequestParam Long utstillingId,
	                          @RequestParam(required = false) List<String> raseNavn,
	                          @RequestParam(required = false) List<Long> raseVinnerDueId,
	                          @RequestParam(required = false) List<String> gruppeNavn,
	                          @RequestParam(required = false) List<Long> gruppeVinnerDueId,
	                          @RequestParam(required = false) Long bisVinnerDueId,
	                          @RequestParam(required = false) Long norgesmesterOppdrett1DueId,
	                          @RequestParam(required = false) Long norgesmesterOppdrett2DueId,
	                          @RequestParam(required = false) Long norgesmesterOppdrett3DueId,
	                          @AuthenticationPrincipal Bruker bruker,
	                          Model model) {
		bruker = sikkerBruker(bruker);
		model.addAttribute("valgtUtstillingId", utstillingId);
		model.addAttribute("duerPerRase", hentDuerPerRase(bruker, utstillingId, null));
		try {
			dommerService.lagreVinnere(
					bruker,
					utstillingId,
					raseNavn,
					raseVinnerDueId,
					gruppeNavn,
					gruppeVinnerDueId,
					bisVinnerDueId,
					norgesmesterOppdrett1DueId,
					norgesmesterOppdrett2DueId,
					norgesmesterOppdrett3DueId
			);
			model.addAttribute("vinnerMelding", "Vinnere er lagret.");
		} catch (RuntimeException e) {
			model.addAttribute("vinnerFeilMelding", e.getMessage());
		}
		leggTilVinnerData(model, bruker, utstillingId);
		return "dommer/dommer_fragments :: dueliste";
	}

	private BedommelseBilde opprettBilde(MultipartFile bilde) {
		if (bilde == null || bilde.isEmpty()) {
			return null;
		}
		if (bilde.getSize() > MAKS_BILDESTORRELSE_BYTES) {
			throw new IllegalArgumentException("Bilde kan maksimalt vaere 5 MB");
		}
		String contentType = bilde.getContentType();
		if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"))) {
			throw new IllegalArgumentException("Kun JPEG, PNG eller WEBP er tillatt");
		}

		try {
			BedommelseBilde bedommelseBilde = new BedommelseBilde();
			bedommelseBilde.setContentType(contentType);
			bedommelseBilde.setFilnavn(bilde.getOriginalFilename() == null ? "ukjent" : bilde.getOriginalFilename());
			bedommelseBilde.setSizeBytes(bilde.getSize());
			bedommelseBilde.setData(bilde.getBytes());
			return bedommelseBilde;
		} catch (IOException e) {
			throw new IllegalArgumentException("Klarte ikke lese opp lastet bilde", e);
		}
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


	private Map<String, List<String>> standardFordelerKommentarerPerKategori() {
		return standardKommentarService.hentKommentarTeksterPerKategori(StandardKommentarType.STANDARD);
	}

	private Map<String, List<String>> standardOnskerKommentarerPerKategori() {
		return standardKommentarService.hentKommentarTeksterPerKategori(StandardKommentarType.ONSKER);
	}

	private Map<String, List<String>> standardFeilKommentarerPerKategori() {
		return standardKommentarService.hentKommentarTeksterPerKategori(StandardKommentarType.FEIL);
	}

	private Map<String, KategoriKommentar> eksisterendeKategorierFraTekst(String lagret,
	                                                                     Map<String, List<String>> standardKommentarerPerKategori) {
		Map<String, KategoriKommentar> eksisterende = new LinkedHashMap<>();
		if (lagret == null || lagret.isBlank()) {
			return eksisterende;
		}
		for (String linje : lagret.split("\\n")) {
			String tekst = trimTilTomTekst(linje);
			if (tekst.isBlank()) {
				continue;
			}
			int skilletegn = tekst.indexOf(':');
			if (skilletegn < 0) {
				continue;
			}
			String visningsnavn = trimTilTomTekst(tekst.substring(0, skilletegn));
			BedommingsKategori kategori = finnKategoriFraVisningsnavn(visningsnavn);
			if (kategori == null) {
				continue;
			}
			String verdi = trimTilTomTekst(tekst.substring(skilletegn + 1));
			String standardKommentar = "";
			String fritekstKommentar = "";
			if (standardKommentarerPerKategori != null) {
				if (verdi.contains(" / ")) {
					String[] deler = verdi.split(" / ", 2);
					standardKommentar = trimTilTomTekst(deler[0]);
					fritekstKommentar = trimTilTomTekst(deler[1]);
				} else if (erStandardKommentarTekst(kategori, verdi, standardKommentarerPerKategori)) {
					standardKommentar = verdi;
				} else {
					fritekstKommentar = verdi;
				}
			} else {
				fritekstKommentar = verdi;
			}
			eksisterende.put(kategori.name(), new KategoriKommentar(standardKommentar, fritekstKommentar));
		}
		return eksisterende;
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
		return eksisterendeStandardKommentarerPerKategori(eksisterendeKategorierPerKategori(bedommelse));
	}

	private Map<String, List<String>> eksisterendeStandardKommentarerPerKategori(Map<String, KategoriKommentar> kategorier) {
		Map<String, List<String>> eksisterende = new LinkedHashMap<>();
		if (kategorier == null || kategorier.isEmpty()) {
			return eksisterende;
		}
		for (Map.Entry<String, KategoriKommentar> entry : kategorier.entrySet()) {
			eksisterende.put(entry.getKey(), splittStandardKommentarer(entry.getValue().getStandardKommentar()));
		}
		return eksisterende;
	}

	private Map<BedommingsKategori, KategoriKommentar> byggKategoriKommentarer(MultiValueMap<String, String> skjemaData,
	                                                                           String standardPrefix,
	                                                                           String fritekstPrefix) {
		Map<BedommingsKategori, KategoriKommentar> kategorier = new EnumMap<>(BedommingsKategori.class);
		for (BedommingsKategori kategori : BedommingsKategori.values()) {
			List<String> valgteStandarder = standardPrefix == null ? List.of() : skjemaData.get(standardPrefix + kategori.name());
			String standard = standardPrefix == null ? "" : joinStandardKommentarer(valgteStandarder);
			String fritekst = fritekstPrefix == null ? "" : trimTilTomTekst(skjemaData.getFirst(fritekstPrefix + kategori.name()));
			if (!standard.isBlank() || !fritekst.isBlank()) {
				kategorier.put(kategori, new KategoriKommentar(standard, fritekst));
			}
		}
		return kategorier;
	}

	private String oppsummerKategorierTilTekst(Map<BedommingsKategori, KategoriKommentar> kategorier) {
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

	private BedommingsKategori finnKategoriFraVisningsnavn(String visningsnavn) {
		return Arrays.stream(BedommingsKategori.values())
				.filter(kategori -> kategori.getVisningsnavn().equals(visningsnavn))
				.findFirst()
				.orElse(null);
	}

	private boolean erStandardKommentarTekst(BedommingsKategori kategori,
	                                        String verdi,
	                                        Map<String, List<String>> standardKommentarerPerKategori) {
		String tekst = trimTilTomTekst(verdi);
		if (tekst.isBlank()) {
			return false;
		}
		List<String> standardKommentarer = standardKommentarerPerKategori.get(kategori.name());
		if (standardKommentarer == null || standardKommentarer.isEmpty()) {
			return false;
		}
		List<String> deler = Arrays.stream(tekst.split(","))
				.map(this::trimTilTomTekst)
				.filter(verdiDel -> !verdiDel.isBlank())
				.toList();
		return !deler.isEmpty() && standardKommentarer.containsAll(deler);
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

	private void leggTilVinnerData(Model model, Bruker bruker, Long utstillingId) {
		try {
			DommerVinnerData vinnerData = dommerService.hentVinnerData(bruker, utstillingId);
			model.addAttribute("klarForVinnerkaring", vinnerData.isKlarForVinnerkaring());
			model.addAttribute("vinnerKandidaterPerRase", vinnerData.getKandidaterPerRase());
			model.addAttribute("vinnerKandidaterPerGruppe", vinnerData.getKandidaterPerGruppe());
			model.addAttribute("alleVinnerKandidater", vinnerData.getAlleKandidater());
			model.addAttribute("valgteRasevinnere", vinnerData.getValgteRasevinnere());
			model.addAttribute("valgteGruppevinnere", vinnerData.getValgteGruppevinnere());
			model.addAttribute("valgtBisVinnerId", vinnerData.getValgtBisVinnerId());
			model.addAttribute("valgtNorgesmesterOppdrett1Id", vinnerData.getValgtNorgesmesterOppdrett1Id());
			model.addAttribute("valgtNorgesmesterOppdrett2Id", vinnerData.getValgtNorgesmesterOppdrett2Id());
			model.addAttribute("valgtNorgesmesterOppdrett3Id", vinnerData.getValgtNorgesmesterOppdrett3Id());
		} catch (RuntimeException e) {
			model.addAttribute("klarForVinnerkaring", false);
			model.addAttribute("vinnerKandidaterPerRase", Map.of());
			model.addAttribute("vinnerKandidaterPerGruppe", Map.of());
			model.addAttribute("alleVinnerKandidater", List.of());
			model.addAttribute("valgteRasevinnere", Map.of());
			model.addAttribute("valgteGruppevinnere", Map.of());
			model.addAttribute("valgtBisVinnerId", null);
			model.addAttribute("valgtNorgesmesterOppdrett1Id", null);
			model.addAttribute("valgtNorgesmesterOppdrett2Id", null);
			model.addAttribute("valgtNorgesmesterOppdrett3Id", null);
		}
	}

	@ModelAttribute("navLocation")
	public String navLocation() {
		return navLocation;
	}

}
