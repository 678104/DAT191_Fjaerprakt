package no.hvl.peristeri.auth;

import no.hvl.peristeri.feature.bruker.Bruker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	List<PasswordResetToken> findByBrukerAndUsedAtIsNull(Bruker bruker);
}

