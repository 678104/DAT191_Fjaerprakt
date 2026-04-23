package no.hvl.peristeri.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.bruker.Bruker;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Bruker bruker;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private LocalDateTime usedAt;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}

	public boolean isUsed() {
		return usedAt != null;
	}
}

