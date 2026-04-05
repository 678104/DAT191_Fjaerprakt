package no.hvl.peristeri.feature.dommer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "standard_kommentar", uniqueConstraints = {
		@UniqueConstraint(name = "uk_standard_kommentar", columnNames = {"kategori", "type", "tekst"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StandardKommentar {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BedommingsKategori kategori;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StandardKommentarType type;

	@Column(nullable = false, length = 500)
	private String tekst;

	public StandardKommentar(BedommingsKategori kategori, StandardKommentarType type, String tekst) {
		this.kategori = kategori;
		this.type = type;
		this.tekst = tekst;
	}
}

