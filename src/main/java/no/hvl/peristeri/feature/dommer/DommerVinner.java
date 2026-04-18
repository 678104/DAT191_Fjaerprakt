package no.hvl.peristeri.feature.dommer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.due.Due;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dommer_vinner", uniqueConstraints = {
		@UniqueConstraint(name = "uk_dommer_vinner", columnNames = {"dommer_paamelding_id", "type", "kategori_navn"})
})
public class DommerVinner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "dommer_paamelding_id", nullable = false)
	private DommerPaamelding dommerPaamelding;

	@ManyToOne(optional = false)
	@JoinColumn(name = "due_id", nullable = false)
	private Due due;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DommerVinnerType type;

	@Column(name = "kategori_navn", nullable = false, length = 120)
	private String kategoriNavn;
}

