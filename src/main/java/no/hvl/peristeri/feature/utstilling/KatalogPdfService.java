package no.hvl.peristeri.feature.utstilling;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.dommer.DommerPaamelding;
import no.hvl.peristeri.feature.dommer.DommerService;
import no.hvl.peristeri.feature.dommer.DommerVinner;
import no.hvl.peristeri.feature.dommer.DommerVinnerRepository;
import no.hvl.peristeri.feature.dommer.DommerVinnerType;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.due.DueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KatalogPdfService {

	private static final String BIS_NAVN = "BIS";
	private static final String NORGESMESTER_OPPDRETT_1 = "NORGESMESTER_OPPDRETT_1";
	private static final String NORGESMESTER_OPPDRETT_2 = "NORGESMESTER_OPPDRETT_2";
	private static final String NORGESMESTER_OPPDRETT_3 = "NORGESMESTER_OPPDRETT_3";

	private final TemplateEngine templateEngine;
	private final UtstillingService utstillingService;
	private final DueService dueService;
	private final DommerService dommerService;
	private final DommerVinnerRepository dommerVinnerRepository;

	@Transactional(readOnly = true)
	public byte[] genererKatalogPdf(Long utstillingId) {
		return genererKatalogPdf(utstillingId, null);
	}

	@Transactional(readOnly = true)
	public byte[] genererKatalogPdf(Long utstillingId, KatalogPdfRedigering redigering) {
		String html = genererKatalogHtml(utstillingId, redigering);
		return renderPdf(html);
	}

	@Transactional(readOnly = true)
	public String genererKatalogHtml(Long utstillingId, KatalogPdfRedigering redigering) {
		Utstilling utstilling = utstillingService.finnUtstillingMedId(utstillingId);
		KatalogPdfRedigering aktivRedigering = redigering == null ? standardRedigering(utstilling) : redigering;
		List<Due> alleDuer = dueService.finnAlleDuerPaameldtUTstilling(utstillingId);
		List<Due> katalogDuer = sorterDuerForKatalog(alleDuer);

		List<DommerPaamelding> dommerliste = dommerService.finnDommerPaameldingerTilUtstilling(utstillingId);
		List<Bruker> utstillere = utstillingService.hentSortertListeAvUtstillereFraUtstilling(utstillingId);
		Map<Long, List<String>> utstillerRaser = utstillingService.hentUtstillereSineRaser(utstillingId);
		List<DommerVinner> vinnere = dommerVinnerRepository.findByDommerPaamelding_Utstilling_IdOrderByTypeAscKategoriNavnAsc(utstillingId);

		Due bisDue = finnBisDue(vinnere);
		Due norgesmester1 = finnBisKategoriDue(vinnere, NORGESMESTER_OPPDRETT_1);
		Due norgesmester2 = finnBisKategoriDue(vinnere, NORGESMESTER_OPPDRETT_2);
		Due norgesmester3 = finnBisKategoriDue(vinnere, NORGESMESTER_OPPDRETT_3);

		List<VinnerLinje> gruppevinnere = vinnere.stream()
		                                        .filter(v -> v.getType() == DommerVinnerType.GRUPPE)
		                                        .map(v -> new VinnerLinje(v.getKategoriNavn(), v.getDue(), fulltDommernavn(v.getDommerPaamelding())))
		                                        .toList();

		List<Due> gullmedaljer = alleDuer.stream()
		                                 .filter(d -> d.getBedommelse() != null && d.getBedommelse().getPoeng() != null)
		                                 .filter(d -> d.getBedommelse().getPoeng() >= 97)
		                                 .sorted(poengSortering())
		                                 .toList();

		List<Due> champions = alleDuer.stream()
		                             .filter(d -> d.getBedommelse() != null && d.getBedommelse().getPoeng() != null)
		                             .filter(d -> d.getBedommelse().getPoeng() >= 96)
		                             .sorted(poengSortering())
		                             .toList();

		Map<String, List<Due>> duerPerRase = katalogDuer.stream()
		                                            .collect(Collectors.groupingBy(
			                                            d -> tomTilUkjent(d.getRase()),
			                                            LinkedHashMap::new,
			                                            Collectors.toList()));

		List<NummerertDue> nummererteDuer = new ArrayList<>();
		for (int i = 0; i < katalogDuer.size(); i++) {
			nummererteDuer.add(new NummerertDue(i + 1, katalogDuer.get(i)));
		}

		Context context = new Context(Locale.of("no", "NO"));
		context.setVariable("utstilling", utstilling);
		context.setVariable("dommere", dommerliste);
		context.setVariable("bisDue", bisDue);
		context.setVariable("bisImage", lagDataUri(bisDue));
		context.setVariable("norgesmester1", norgesmester1);
		context.setVariable("norgesmester2", norgesmester2);
		context.setVariable("norgesmester3", norgesmester3);
		context.setVariable("gruppevinnere", gruppevinnere);
		context.setVariable("gullmedaljer", gullmedaljer);
		context.setVariable("champions", champions);
		context.setVariable("duerPerRase", duerPerRase);
		context.setVariable("nummererteDuer", nummererteDuer);
		context.setVariable("utstillere", utstillere);
		context.setVariable("utstillerRaser", utstillerRaser);
		context.setVariable("generertTidspunkt", LocalDateTime.now());
		context.setVariable("harTrioOgParData", false);
		context.setVariable("katalogKommentar", aktivRedigering.katalogKommentar());
		context.setVariable("forsideTittel", aktivRedigering.forsideTittel());
		context.setVariable("forsideArrangor", aktivRedigering.forsideArrangor());
		context.setVariable("forsideSted", aktivRedigering.forsideSted());
		context.setVariable("forsideDato", aktivRedigering.forsideDato());
		context.setVariable("dommerlisteOverskrift", aktivRedigering.dommerlisteOverskrift());
		context.setVariable("bisOverskrift", aktivRedigering.bisOverskrift());
		context.setVariable("mestereOverskrift", aktivRedigering.mestereOverskrift());
		context.setVariable("gruppevinnereOverskrift", aktivRedigering.gruppevinnereOverskrift());
		context.setVariable("championsOverskrift", aktivRedigering.championsOverskrift());
		context.setVariable("utstillereOverskrift", aktivRedigering.utstillereOverskrift());

		return templateEngine.process("print/katalog", context);
	}

	public KatalogPdfRedigering standardRedigering(Utstilling utstilling) {
		String datoTekst = "";
		if (utstilling != null && utstilling.getDatoRange() != null && utstilling.getDatoRange().getStartDate() != null && utstilling.getDatoRange().getEndDate() != null) {
			datoTekst = utstilling.getDatoRange().getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
					+ " - "
					+ utstilling.getDatoRange().getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}

		return new KatalogPdfRedigering(
			utstilling != null ? utstilling.getTittel() : "Utstillingskatalog",
			utstilling != null ? utstilling.getArrangoer() : "",
			utstilling != null ? utstilling.getAdresse() : "",
			datoTekst,
			"Katalogen er automatisk generert basert paa registrerte bedommelser og vinnerdata i Peristeri.",
			"Dommerliste og kataloginfo",
			"Best in Show",
			"Mestere og spesialpriser",
			"Gruppevinnere og gullmedaljer",
			"Champions",
			"Utstillere"
		);
	}

	private byte[] renderPdf(String html) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(html, null);
			builder.toStream(outputStream);
			builder.run();
			return outputStream.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("Klarte ikke aa generere PDF-katalog", e);
		}
	}

	private Comparator<Due> poengSortering() {
		return Comparator
				.comparing((Due d) -> d.getBedommelse().getPoeng(), Comparator.reverseOrder())
				.thenComparing(d -> d.getBurnummer() == null ? Integer.MAX_VALUE : d.getBurnummer())
				.thenComparing(d -> tomTilUkjent(d.getRase()));
	}

	private List<Due> sorterDuerForKatalog(List<Due> duer) {
		Collator collator = Collator.getInstance(Locale.of("no", "NO"));
		collator.setStrength(Collator.PRIMARY);

		return duer.stream()
		          .sorted((a, b) -> {
			          boolean aNorsk = erNorskRase(a.getRase());
			          boolean bNorsk = erNorskRase(b.getRase());
			          if (aNorsk != bNorsk) {
				          return aNorsk ? -1 : 1;
			          }
			          int raseCompare = collator.compare(tomTilUkjent(a.getRase()), tomTilUkjent(b.getRase()));
			          if (raseCompare != 0) {
				          return raseCompare;
			          }
			          int fargeCompare = collator.compare(tomTilUkjent(a.getFarge()), tomTilUkjent(b.getFarge()));
			          if (fargeCompare != 0) {
				          return fargeCompare;
			          }
			          int variantCompare = collator.compare(tomTilUkjent(a.getVariant()), tomTilUkjent(b.getVariant()));
			          if (variantCompare != 0) {
				          return variantCompare;
			          }
			          return Integer.compare(
				          a.getBurnummer() == null ? Integer.MAX_VALUE : a.getBurnummer(),
				          b.getBurnummer() == null ? Integer.MAX_VALUE : b.getBurnummer());
		          })
		          .toList();
	}

	private String lagDataUri(Due due) {
		if (due == null || due.getBedommelse() == null || due.getBedommelse().getBilde() == null) {
			return null;
		}
		byte[] data = due.getBedommelse().getBilde().getData();
		String contentType = due.getBedommelse().getBilde().getContentType();
		if (data == null || data.length == 0 || contentType == null || contentType.isBlank()) {
			return null;
		}
		return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(data);
	}

	private Due finnBisDue(List<DommerVinner> vinnere) {
		return finnBisKategoriDue(vinnere, BIS_NAVN);
	}

	private Due finnBisKategoriDue(List<DommerVinner> vinnere, String kategoriNavn) {
		return vinnere.stream()
		              .filter(v -> v.getType() == DommerVinnerType.BIS)
		              .filter(v -> Objects.equals(v.getKategoriNavn(), kategoriNavn))
		              .map(DommerVinner::getDue)
		              .findFirst()
		              .orElse(null);
	}

	private String fulltDommernavn(DommerPaamelding dp) {
		if (dp == null) {
			return "Ukjent dommer";
		}
		return dp.getDommer().getFornavn() + " " + dp.getDommer().getEtternavn();
	}

	private boolean erNorskRase(String rase) {
		if (rase == null) {
			return false;
		}
		String lower = rase.toLowerCase(Locale.ROOT);
		return lower.startsWith("norsk ") || lower.contains(" norsk");
	}

	private String tomTilUkjent(String value) {
		if (value == null || value.isBlank()) {
			return "Ukjent";
		}
		return value.trim();
	}

	public record NummerertDue(int nummer, Due due) {
	}

	public record VinnerLinje(String kategori, Due due, String dommerNavn) {
	}

	public record KatalogPdfRedigering(
		String forsideTittel,
		String forsideArrangor,
		String forsideSted,
		String forsideDato,
		String katalogKommentar,
		String dommerlisteOverskrift,
		String bisOverskrift,
		String mestereOverskrift,
		String gruppevinnereOverskrift,
		String championsOverskrift,
		String utstillereOverskrift
	) {
	}
}


