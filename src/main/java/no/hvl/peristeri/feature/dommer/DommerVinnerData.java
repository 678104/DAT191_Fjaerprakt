package no.hvl.peristeri.feature.dommer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import no.hvl.peristeri.feature.due.Due;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class DommerVinnerData {
	private final boolean klarForVinnerkaring;
	private final Map<String, List<Due>> kandidaterPerRase;
	private final Map<String, List<Due>> kandidaterPerGruppe;
	private final List<Due> alleKandidater;
	private final Map<Long, String> oppdretterKandidater;
	private final Map<String, Long> valgteRasevinnere;
	private final Map<String, Long> valgteGruppevinnere;
	private final Long valgtBisVinnerId;
	private final Long valgtNorgesmesterOppdrett1Id;
	private final Long valgtNorgesmesterOppdrett2Id;
	private final Long valgtNorgesmesterOppdrett3Id;
}

