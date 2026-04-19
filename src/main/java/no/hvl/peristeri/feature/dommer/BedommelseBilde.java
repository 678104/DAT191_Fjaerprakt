package no.hvl.peristeri.feature.dommer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BedommelseBilde {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bedommelse_id", nullable = false, unique = true)
	private Bedommelse bedommelse;

	@Column(nullable = false)
	private String contentType;

	@Column(nullable = false)
	private String filnavn;

	@Column(nullable = false)
	private Long sizeBytes;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(nullable = false)
	private byte[] data;

	@Column(nullable = false)
	private LocalDateTime opprettetTidspunkt;

	@PrePersist
	protected void onCreate() {
		if (opprettetTidspunkt == null) {
			opprettetTidspunkt = LocalDateTime.now();
		}
	}
}

