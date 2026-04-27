package no.hvl.peristeri.feature.due;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.dommer.DommerPaamelding;
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
	private String  rase;
	private String  farge;
	private String  variant;
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

	@ManyToOne
	@JoinColumn(name = "tildelt_dommer_paamelding_id")
	private DommerPaamelding tildeltDommer;

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

	@Transient
	public DueKlasse getKlasse() {
		return Boolean.TRUE.equals(alder) ? DueKlasse.SENIOR : DueKlasse.JUNIOR;
	}
	
	public void setRingnummer(String ringnummer) {
		String[] split = ringnummer.split("-");
		if (split.length != 2) {
			throw new IllegalArgumentException("Ringnummer må være på format 'løpenummer-årskull'");
		}
		this.lopenummer = split[0];
		this.aarstall   = split[1];
	}


	public Due(String rase, String farge, String variant, Boolean kjonn, Boolean alder, Boolean ikkeEget) {
		this.rase     = rase;
		this.farge    = farge;
		this.variant  = variant;
		this.kjonn    = kjonn;
		this.alder    = alder;
		this.ikkeEget = ikkeEget;
	}

	public Due(String ringnummer, String rase, String farge, String variant, Boolean kjonn, Boolean alder,
	           Boolean ikkeEget) {
		setRingnummer(ringnummer);
		this.rase       = rase;
		this.farge      = farge;
		this.variant    = variant;
		this.kjonn      = kjonn;
		this.alder      = alder;
		this.ikkeEget   = ikkeEget;
	}

	public Due(String ringnummer, String rase, String farge, String variant, Boolean kjonn, Boolean alder,
	           Boolean ikkeEget, Paamelding paamelding) {
		setRingnummer(ringnummer);
		this.rase       = rase;
		this.farge      = farge;
		this.variant    = variant;
		this.kjonn      = kjonn;
		this.alder      = alder;
		this.ikkeEget   = ikkeEget;
		this.paamelding = paamelding;
	}


}
