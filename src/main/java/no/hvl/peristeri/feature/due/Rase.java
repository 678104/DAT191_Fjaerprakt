package no.hvl.peristeri.feature.due;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rase", uniqueConstraints = @UniqueConstraint(columnNames = {"navn", "gruppe"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String navn;

	@Column(nullable = false)
	private String gruppe;

	public Rase(String navn, String gruppe) {
		this.navn = navn;
		this.gruppe = gruppe;
	}

	@Transient
	public String getVisningsnavn() {
		return navn + " (" + gruppe + ")";
	}
}


