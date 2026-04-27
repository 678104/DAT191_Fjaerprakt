package no.hvl.peristeri.feature.paamelding;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.duekatalog.DueKatalogService;
import no.hvl.peristeri.feature.duekatalog.DueRase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KassekortService {

	private final TemplateEngine templateEngine;
	private final PaameldingService paameldingService;
	private final DueKatalogService dueKatalogService;

	@Transactional(readOnly = true)
	public byte[] genererKassekortPdf(Long paameldingId) {
		Paamelding paamelding = paameldingService.hentPaamelding(paameldingId);
		String html = genererKassekortHtml(paamelding);
		return renderPdf(html);
	}

	@Transactional(readOnly = true)
	public String genererKassekortHtml(Paamelding paamelding) {
		List<KassekortRaseKort> kassekort = byggKassekortPerRase(paamelding);

		BigDecimal totalAvgift = paamelding.getPaameldingsAvgift() == null ? BigDecimal.ZERO : paamelding.getPaameldingsAvgift();
		BigDecimal prisPerDue = paamelding.getUtstilling() != null && paamelding.getUtstilling().getDuePris() != null
				? paamelding.getUtstilling().getDuePris()
				: BigDecimal.ZERO;

		Context context = new Context(Locale.of("no", "NO"));
		context.setVariable("paamelding", paamelding);
		context.setVariable("kassekort", kassekort);
		context.setVariable("totalAvgift", totalAvgift);
		context.setVariable("prisPerDue", prisPerDue);
		context.setVariable("antallDuer", paamelding.getDuer() == null ? 0 : paamelding.getDuer().size());
		context.setVariable("generertTidspunkt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

		return templateEngine.process("print/kassekort", context);
	}

	@Transactional(readOnly = true)
	public List<KassekortRaseKort> byggKassekortPerRase(Paamelding paamelding) {
		if (paamelding == null || paamelding.getDuer() == null || paamelding.getDuer().isEmpty()) {
			return List.of();
		}

		Map<String, String> gruppePerRase = byggGruppePerRaseMap();
		Map<String, List<Due>> duerPerRase = paamelding.getDuer().stream()
				.collect(Collectors.groupingBy(
						d -> tomTilStrek(d.getRase()),
						LinkedHashMap::new,
						Collectors.toList()));

		return duerPerRase.entrySet().stream()
				.map(entry -> {
					String rase = entry.getKey();
					String gruppe = gruppePerRase.getOrDefault(rase, "-");
					List<KassekortDueDetalj> duer = entry.getValue().stream()
							.map(this::mapDueDetalj)
							.toList();
					return new KassekortRaseKort(gruppe, rase, duer);
				})
				.toList();
	}

	private KassekortDueDetalj mapDueDetalj(Due due) {
		String kjonnAlder = formatKjonnAlder(due.getKjonn(), due.getAlder());
		String avlstatus = Boolean.TRUE.equals(due.getIkkeEget()) ? "Ikke eget avl" : "Eget avl";
		String ringnummer = due.getRingnummer() == null || due.getRingnummer().isBlank() ? "-" : due.getRingnummer();
		return new KassekortDueDetalj(
				tomTilStrek(due.getFarge()),
				tomTilStrek(due.getVariant()),
				kjonnAlder,
				avlstatus,
				ringnummer
		);
	}

	private Map<String, String> byggGruppePerRaseMap() {
		Map<String, String> resultat = new LinkedHashMap<>();
		for (DueRase dueRase : dueKatalogService.finnAlleRaser()) {
			if (dueRase.getNavn() == null || dueRase.getNavn().isBlank()) {
				continue;
			}
			String gruppeNavn = dueRase.getGruppe() != null ? tomTilStrek(dueRase.getGruppe().getNavn()) : "-";
			resultat.putIfAbsent(dueRase.getNavn(), gruppeNavn);
		}
		return resultat;
	}

	private String formatKjonnAlder(Boolean hann, Boolean eldre) {
		String kjonn = Boolean.TRUE.equals(hann) ? "Han" : "Hun";
		String alder = Boolean.TRUE.equals(eldre) ? "Eldre" : "Ung";
		return kjonn + ", " + alder;
	}

	private String tomTilStrek(String verdi) {
		return verdi == null || verdi.isBlank() ? "-" : verdi;
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
			String detaljer = e.getMessage() == null ? "ukjent feil" : e.getMessage();
			throw new IllegalStateException("Klarte ikke å generere kassekort-PDF: " + detaljer, e);
		}
	}

	public record KassekortRaseKort(
			String gruppe,
			String rase,
			List<KassekortDueDetalj> duer
	) {
	}

	public record KassekortDueDetalj(
			String farge,
			String variant,
			String kjonnAlder,
			String avlstatus,
			String ringnummer
	) {
	}

}

