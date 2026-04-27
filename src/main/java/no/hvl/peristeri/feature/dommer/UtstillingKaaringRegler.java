package no.hvl.peristeri.feature.dommer;

import no.hvl.peristeri.feature.utstilling.UtstillingType;

public record UtstillingKaaringRegler(
	boolean harRasevinner,
	boolean harGruppevinner,
	boolean harBis,
	boolean harNorgesmesterOppdrett,
	boolean harNorgesmesterBesteTrioSenior,
	boolean harNorgesmesterBesteParJunior,
	boolean harEePlakett
) {
	public static UtstillingKaaringRegler forType(UtstillingType utstillingType) {
		if (utstillingType == UtstillingType.LANDSUTSTILLING) {
			return new UtstillingKaaringRegler(true, false, false, true, true, true, true);
		}
		return new UtstillingKaaringRegler(false, true, true, false, false, false, false);
	}
}

