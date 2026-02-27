package no.hvl.peristeri.feature.paamelding;

import jakarta.transaction.Transactional;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.utstilling.Utstilling;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaameldingService {
	/**
	 * Denne metoden forventer at påmeldingen er rikitg koblet opp før den brukes
	 *
	 * @param paamelding Påmeldingen som skal legges til
	 *
	 * @return Den lagrede påmeldingen
	 */
	Paamelding manueltLeggTilPaamelding(Paamelding paamelding);

	@Transactional
	Paamelding leggTilPaamelding(Long utstillerId, Long utstillingId, DueDTOList duerDTO,
	                             BigDecimal paameldingsAvgift);

	/**
	 * Henter påmeldinger til en bruker, og deler dem opp i kommende og tidligere påmeldinger
	 *
	 * @param bruker Brukeren som skal hentes påmeldinger for
	 *
	 * @return En map med kommende og tidligere påmeldinger
	 */
	Map<String, List<Paamelding>> hentBrukerSinePaameldinger(Bruker bruker);

	Paamelding hentPaamelding(Long id);

	List<Due> konverterDueDTOtilDue(DueDTO dueDTO);

	Integer antallDuer(List<DueDTO> liste);

	BigDecimal beregnTotalPris(Integer antallDuer, BigDecimal utstilling);

	Paamelding erBrukerPaameldtUtstilling(Bruker bruker, Utstilling utstilling);

	boolean sjekkOmBrukerAlleredeErPaameldt(Bruker bruker, Utstilling utstilling);
}
