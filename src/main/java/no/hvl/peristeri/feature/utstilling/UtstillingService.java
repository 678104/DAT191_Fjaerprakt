package no.hvl.peristeri.feature.utstilling;

import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface UtstillingService {
	/**
	 * Henter den aktive utstillingen.<br>
	 * Hvis
	 *
	 * @return Den aktive utstillingen, eller null dersom ingen utstilling er aktiv.
	 */
	Utstilling finnAktivUtstilling();

	/**
	 * Setter utstilling med id til aktiv.<br>
	 * Denne metoden oppdaterer også aktiv status i databasen.
	 *
	 * @param utstillingId Id til utstillingen som skal være aktiv
	 *
	 * @return Den nye aktiv utstillingen, eller null dersom utstilling med id ikke finnes.
	 */
	@Transactional
	Utstilling setAktivUtstilling(Long utstillingId);

	/**
	 * Fjerner den aktive utstillingen.<br>
	 * Denne metoden oppdaterer også aktiv status i databasen.
	 */
	@Transactional
	void fjernAktivUtstilling();

	Utstilling leggTilUtstilling(Utstilling nyUtstilling);

	Utstilling oppdaterUtstilling(Long id, Utstilling oppdatertUtstilling);

	Utstilling finnUtstillingMedId(Long id);

	List<Utstilling> addAll(List<Utstilling> utstillinger);

	List<Utstilling> hentAlleUtstillinger();

	List<Utstilling> finnIkkeTidligereUtstillinger();

	List<Utstilling> finnUtstillingerMedMulighetForPaamelding();

	List<Utstilling> finnKommendeUtstillinger();

	List<Utstilling> finnTidligereUtstillinger();

	Utstilling oppdaterSorterteRaser(Long id, String raser);

	@Transactional
	Utstilling genererBurnumre(Utstilling utstilling);

	List<Due> finnAlleDuerFraUtstillingSomHarBedommelse(Long id);

	Map<String, List<String>> hentFargeForRaseFraUtstilling(Utstilling utstilling);

	/**
	 * Henter status på bedømmelser for en utstilling.<br>
	 * Status er antall duer påmeldt og antall duer som er blitt bedømt.<br>
	 *
	 * @param utstilling Utstillingen som skal sjekkes.
	 *
	 * @return Liste med 2 elementer, der første element er antall duer påmeldt og andre element er antall duer som er blitt bedømt.
	 */
	List<Long> hentStatusPaaBedommelser(Utstilling utstilling);

	List<Bruker> hentSortertListeAvUtstillereFraUtstilling(Long utstillingId);

	Map<Long, List<String>> hentUtstillereSineRaser(Long utstillingId);

	Map<String, List<String>> hentVarianterForRase(Long utstillingId);

	Map<String, List<String>> hentFargerForVarianter(Long utstillingId);
}
