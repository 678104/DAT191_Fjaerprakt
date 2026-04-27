package no.hvl.peristeri.feature.utstilling;

public enum UtstillingType {
	HOSTUTSTILLING("Høstutstilling"),
	LANDSUTSTILLING("Landsutstilling");

	private final String visningsnavn;

	UtstillingType(String visningsnavn) {
		this.visningsnavn = visningsnavn;
	}

	public String getVisningsnavn() {
		return visningsnavn;
	}
}

