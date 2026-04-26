package no.hvl.peristeri.feature.dommer;

import jakarta.persistence.*;
import lombok.*;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.util.RaseStringHjelper;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Table(name = "dommer_paamelding")
public class DommerPaamelding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "utstilling_id", nullable = false)
	private Utstilling utstilling;

	@NonNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "dommer_id", nullable = false)
	private Bruker dommer;

	/**
	 * Denne er tenkt å være en "liste" av raser dommeren skal bedømme.
	 *
	 * @see RaseStringHjelper Se RaseStringHjelper for mer info.
	 */
	@Column(length = 1000)
	private String raser = "";

}
