package no.hvl.peristeri.feature.due;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variant_lookup", uniqueConstraints = @UniqueConstraint(columnNames = "navn"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Variant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String navn;

	public Variant(String navn) {
		this.navn = navn;
	}
}

