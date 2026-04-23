package no.hvl.peristeri.auth;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PasswordResetService {

	private final PasswordResetTokenRepository tokenRepository;
	private final BrukerRepository brukerRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetProperties properties;
	private final PasswordResetAuditService auditService;

	@Transactional
	public Optional<String> requestPasswordReset(String epost) {
		if (epost == null || epost.isBlank()) {
			auditService.resetRejected("empty_email");
			return Optional.empty();
		}

		Optional<Bruker> brukerOptional = brukerRepository.findByEpost(epost.trim());
		if (brukerOptional.isEmpty()) {
			auditService.resetRejected("unknown_email");
			return Optional.empty();
		}

		Bruker bruker = brukerOptional.get();
		invalidateOpenTokens(bruker);

		String tokenValue = UUID.randomUUID() + "-" + UUID.randomUUID();
		LocalDateTime now = LocalDateTime.now();

		PasswordResetToken token = new PasswordResetToken();
		token.setTokenHash(hashToken(tokenValue));
		token.setBruker(bruker);
		token.setCreatedAt(now);
		token.setExpiresAt(now.plusMinutes(Math.max(1, properties.getTokenTtlMinutes())));

		tokenRepository.save(token);
		auditService.resetRequested(bruker);
		return Optional.of(tokenValue);
	}

	@Transactional(readOnly = true)
	public boolean isResetTokenValid(String token) {
		return findValidToken(token).isPresent();
	}

	@Transactional
	public boolean resetPassword(String token, String newPassword) {
		if (newPassword == null || newPassword.length() < 4) {
			auditService.resetRejected("password_too_short");
			return false;
		}

		Optional<PasswordResetToken> validTokenOptional = findValidToken(token);
		if (validTokenOptional.isEmpty()) {
			return false;
		}

		PasswordResetToken validToken = validTokenOptional.get();
		Bruker bruker = validToken.getBruker();
		bruker.setPassword(passwordEncoder.encode(newPassword));
		brukerRepository.save(bruker);

		validToken.setUsedAt(LocalDateTime.now());
		tokenRepository.save(validToken);
		auditService.resetTokenUsed(bruker);
		return true;
	}

	private Optional<PasswordResetToken> findValidToken(String token) {
		if (token == null || token.isBlank()) {
			auditService.resetRejected("empty_token");
			return Optional.empty();
		}

		Optional<PasswordResetToken> tokenOptional = tokenRepository.findByTokenHash(hashToken(token));
		if (tokenOptional.isEmpty()) {
			auditService.resetTokenInvalid(tokenPrefix(token));
			return Optional.empty();
		}

		PasswordResetToken passwordResetToken = tokenOptional.get();
		if (passwordResetToken.isUsed()) {
			auditService.resetRejected("token_used");
			return Optional.empty();
		}
		if (passwordResetToken.isExpired(LocalDateTime.now())) {
			auditService.resetRejected("token_expired");
			return Optional.empty();
		}
		return tokenOptional;
	}

	private void invalidateOpenTokens(Bruker bruker) {
		List<PasswordResetToken> openTokens = tokenRepository.findByBrukerAndUsedAtIsNull(bruker);
		if (openTokens.isEmpty()) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		openTokens.forEach(token -> token.setUsedAt(now));
		tokenRepository.saveAll(openTokens);
	}

	private String hashToken(String rawToken) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec keySpec = new SecretKeySpec(properties.getTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			mac.init(keySpec);
			byte[] digest = mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Could not hash password reset token", ex);
		}
	}

	private String tokenPrefix(String token) {
		if (token == null || token.isBlank()) {
			return "unknown";
		}
		return token.length() <= 8 ? token : token.substring(0, 8);
	}
}

