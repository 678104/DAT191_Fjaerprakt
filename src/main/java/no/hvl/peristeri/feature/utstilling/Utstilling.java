package no.hvl.peristeri.feature.utstilling;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.common.DateRange;
import no.hvl.peristeri.common.validation.ValidDateRange;
import no.hvl.peristeri.feature.dommer.DommerPaamelding;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.util.RaseStringHjelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity(name = "Utstilling")
public class Utstilling {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	private Long id;

	@AttributeOverrides({
			@AttributeOverride(name = "startDate", column = @Column(name = "date_start")),
			@AttributeOverride(name = "endDate", column = @Column(name = "date_end"))
	})
	@ValidDateRange
	@Embedded
	private DateRange datoRange;

	@Column(name = "paameldings_frist")
	private LocalDate paameldingsFrist;

	@Column(name = "paamelding_aapnet")
	private Boolean paameldingAApnet;

	private String arrangoer;
	private String adresse;
	private String postnummer;
	private String poststed;
	private String tittel;

	@Column(name = "beskrivelse", length = 1000)
	private String beskrivelse;

	@Column(name = "due_pris")
	private BigDecimal duePris;

	/**
	 * Sortering av raser i utstillingen. Dette er en liste seperert med "|".
	 *
	 * @see RaseStringHjelper
	 */
	@Column(name = "rase_sortering")
	private String raseSortering = "";

	@NotNull
	@Column(name = "har_burnumre")
	private Boolean harBurnumre = false;

	@Column(name = "aktiv", nullable = false)
	private Boolean aktiv = false;

	@OneToMany(mappedBy = "utstilling", cascade = CascadeType.ALL)
	private List<Paamelding> paameldinger;

	@OneToMany(mappedBy = "utstilling", cascade = CascadeType.ALL)
	private List<DommerPaamelding> dommere;


	public Utstilling(String arrangoer, String adresse, String tittel, String beskrivelse, LocalDate startDate,
	                  LocalDate endDate,
	                  LocalDate paameldingsFrist, Double duePris) {
		this.arrangoer        = arrangoer;
		this.adresse          = adresse;
		this.tittel           = tittel;
		this.beskrivelse      = beskrivelse;
		this.datoRange        = new DateRange(startDate, endDate);
		this.paameldingsFrist = paameldingsFrist;
		this.paameldingAApnet = false;
		this.duePris          = BigDecimal.valueOf(duePris);
	}

	public Utstilling(String arrangoer, String adresse, String tittel, String beskrivelse, LocalDate startDate,
	                  LocalDate endDate,
	                  LocalDate paameldingsFrist, BigDecimal duePris) {
		this.arrangoer        = arrangoer;
		this.adresse          = adresse;
		this.tittel           = tittel;
		this.beskrivelse      = beskrivelse;
		this.datoRange        = new DateRange(startDate, endDate);
		this.paameldingsFrist = paameldingsFrist;
		this.paameldingAApnet = false;
		this.duePris          = duePris;
	}

}
