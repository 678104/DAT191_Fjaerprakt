package no.hvl.peristeri.feature.paamelding;

public record DueDTO(
		Integer radId,
		String rase,
		String farge,
		String variant,
		Integer hannerUng,
		Integer hannerEldre,
		Integer hunnerUng,
		Integer hunnerEldre,
		Boolean ikkeEget
) {

	// Konstruktør som setter alle null-verdier til 0 eller false for å unngå NullPointerException
	public DueDTO {
		hannerUng   = hannerUng == null ? 0 : hannerUng;
		hannerEldre = hannerEldre == null ? 0 : hannerEldre;
		hunnerUng   = hunnerUng == null ? 0 : hunnerUng;
		hunnerEldre = hunnerEldre == null ? 0 : hunnerEldre;
		ikkeEget    = ikkeEget != null && ikkeEget;
	}
}
