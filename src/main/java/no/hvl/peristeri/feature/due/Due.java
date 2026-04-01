package no.hvl.peristeri.feature.due;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.dommer.Bedommelse;
import no.hvl.peristeri.feature.paamelding.Paamelding;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Due {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long    id;

	@ManyToOne
	@JoinColumn(name = "rase_id")
	private Rase raseLookup;

	@ManyToOne
	@JoinColumn(name = "farge_id")
	private Farge fargeLookup;

	@ManyToOne
	@JoinColumn(name = "variant_id")
	private Variant variantLookup;
	// Hannduer er true og hunnduer er false
	private Boolean kjonn;
	// Eldre duer er true og unge duer er false
	private Boolean alder;
	private Boolean ikkeEget;
	
	private String lopenummer;
	private String aarstall;

	@Positive
	private Integer burnummer;

	@OneToOne(mappedBy = "due", cascade = CascadeType.ALL)
	private Bedommelse bedommelse;

	// En due må ha en paamelding
	@ManyToOne
	@JoinColumn(name = "paamelding_id")
	private Paamelding paamelding;
	
	public String getRingnummer() {
		if (lopenummer == null || aarstall == null) {
			return null;
		}
		return lopenummer + "-" + aarstall;
	}
	
	public void setRingnummer(String ringnummer) {
		String[] split = ringnummer.split("-");
		if (split.length != 2) {
			throw new IllegalArgumentException("Ringnummer må være på format 'løpenummer-årskull'");
		}
		this.lopenummer = split[0];
		this.aarstall   = split[1];
	}

	@Transient
	public String getRase() {
		return raseLookup == null ? null : raseLookup.getVisningsnavn();
	}

	@Transient
	public String getFarge() {
		return fargeLookup == null ? null : fargeLookup.getNavn();
	}

	@Transient
	public String getVariant() {
		return variantLookup == null ? null : variantLookup.getNavn();
	}


	public Due(Rase raseLookup, Farge fargeLookup, Variant variantLookup, Boolean kjonn, Boolean alder,
	           Boolean ikkeEget) {
		this.raseLookup = raseLookup;
		this.fargeLookup = fargeLookup;
		this.variantLookup = variantLookup;
		this.kjonn    = kjonn;
		this.alder    = alder;
		this.ikkeEget = ikkeEget;
	}

	public Due(String ringnummer, Rase raseLookup, Farge fargeLookup, Variant variantLookup, Boolean kjonn,
	           Boolean alder,
	           Boolean ikkeEget) {
		setRingnummer(ringnummer);
		this.raseLookup = raseLookup;
		this.fargeLookup = fargeLookup;
		this.variantLookup = variantLookup;
		this.kjonn      = kjonn;
		this.alder      = alder;
		this.ikkeEget   = ikkeEget;
	}

	public Due(String ringnummer, Rase raseLookup, Farge fargeLookup, Variant variantLookup, Boolean kjonn,
	           Boolean alder,
	           Boolean ikkeEget, Paamelding paamelding) {
		setRingnummer(ringnummer);
		this.raseLookup = raseLookup;
		this.fargeLookup = fargeLookup;
		this.variantLookup = variantLookup;
		this.kjonn      = kjonn;
		this.alder      = alder;
		this.ikkeEget   = ikkeEget;
		this.paamelding = paamelding;
	}


}
