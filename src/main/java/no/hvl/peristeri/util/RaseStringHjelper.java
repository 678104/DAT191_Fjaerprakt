package no.hvl.peristeri.util;

import no.hvl.peristeri.feature.dommer.DommerPaamelding;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

/**
 * Hjelpeklasse for å håndtere rase tekststrenger.
 * Denne klassen håndterer å sette inn og hente ut raser fra en String.
 * Rasene er separert med pipe (|) når de hentes ut og når de settes inn.
 */
@Component("RaseStringHjelper")
public class RaseStringHjelper {

	/**
	 * Henter ut raser fra en DommerPaamelding.
	 *
	 * @param dp DommerPaamelding med raser.
	 *
	 * @return Liste med raser.
	 */
	public static List<String> hentUtRaser(DommerPaamelding dp) {
		return konverterTilListe(dp.getRaser());
	}

	/**
	 * Setter inn raser i en DommerPaamelding.
	 *
	 * @param dp    DommerPaamelding som skal settes inn.
	 * @param raser Liste med Raser som skal settes inn.
	 */
	public static void settInnRaser(DommerPaamelding dp, List<String> raser) {
		dp.setRaser(konverterTilString(raser));
	}

	/**
	 * Henter ut raser fra en Utstilling.
	 *
	 * @param u Utstilling med raser.
	 *
	 * @return Liste med raser.
	 */
	public static List<String> hentUtRaser(Utstilling u) {
		return konverterTilListe(u.getRaseSortering());
	}

	/**
	 * Setter inn raser i en Utstilling.
	 *
	 * @param u     Utstilling med raser.
	 * @param raser Liste med Raser som skal settes inn.
	 */
	public static void settInnRaser(Utstilling u, List<String> raser) {
		u.setRaseSortering(konverterTilString(raser));
	}

	/**
	 * Setter inn raser i en String.
	 *
	 * @param raser Liste med Raser som skal settes inn.
	 *
	 * @return String med raser separert med pipe "|".
	 */
	public static String konverterTilString(List<String> raser) {
		StringBuilder sb    = new StringBuilder();
		boolean       first = true;
		for (String rase : raser) {
			if (rase == null || rase.isBlank()) {
				continue;
			}
			if (!first) {
				sb.append("|");
			} else {
				first = false;
			}
			sb.append(rase);
		}
		return sb.toString();
	}

	/**
	 * Henter ut raser fra en String.
	 *
	 * @param raser String med raser separert med pipe "|".
	 *
	 * @return Liste med raser.
	 */
	public static List<String> konverterTilListe(String raser) {
		if (raser != null && !raser.isBlank()) {
			String[] raserArray = raser.split("\\|");
			return Stream.of(raserArray)
			             .filter(rase -> rase != null && !rase.isBlank())
			             .toList();
		}
		return List.of();
	}

}
