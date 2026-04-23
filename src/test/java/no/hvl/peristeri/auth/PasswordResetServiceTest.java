package no.hvl.peristeri.auth;

import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

	@Mock
	private PasswordResetTokenRepository tokenRepository;

	@Mock
	private BrukerRepository brukerRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private PasswordResetProperties properties;

	@Mock
	private PasswordResetAuditService auditService;

	@InjectMocks
	private PasswordResetService passwordResetService;

	private Bruker bruker;

	@BeforeEach
	void setup() {
		bruker = new Bruker();
		bruker.setId(1L);
		bruker.setEpost("test@example.com");
		bruker.setPassword("oldHash");
	}

	@Test
	void requestPasswordReset_whenBrukerExists_returnsTokenAndSaves() {
		when(properties.getTokenTtlMinutes()).thenReturn(30);
		when(properties.getTokenSecret()).thenReturn("test-secret");
		when(brukerRepository.findByEpost("test@example.com")).thenReturn(Optional.of(bruker));
		when(tokenRepository.findByBrukerAndUsedAtIsNull(bruker)).thenReturn(List.of());

		Optional<String> token = passwordResetService.requestPasswordReset("test@example.com");

		assertTrue(token.isPresent());
		ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(tokenRepository).save(captor.capture());
		assertNotEquals(token.get(), captor.getValue().getTokenHash());
		assertEquals(64, captor.getValue().getTokenHash().length());
		verify(auditService).resetRequested(bruker);
	}

	@Test
	void requestPasswordReset_whenBrukerMissing_returnsEmpty() {
		when(brukerRepository.findByEpost("ukjent@example.com")).thenReturn(Optional.empty());

		Optional<String> token = passwordResetService.requestPasswordReset("ukjent@example.com");

		assertTrue(token.isEmpty());
		verify(tokenRepository, never()).save(any(PasswordResetToken.class));
		verify(auditService).resetRejected("unknown_email");
	}

	@Test
	void isResetTokenValid_whenTokenIsExpired_returnsFalse() {
		PasswordResetToken token = new PasswordResetToken();
		token.setTokenHash("hash-abc");
		token.setCreatedAt(LocalDateTime.now().minusHours(1));
		token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
		token.setBruker(bruker);

		when(properties.getTokenSecret()).thenReturn("test-secret");
		when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

		assertFalse(passwordResetService.isResetTokenValid("abc"));
	}

	@Test
	void resetPassword_whenTokenValid_updatesPasswordAndMarksTokenUsed() {
		PasswordResetToken token = new PasswordResetToken();
		token.setTokenHash("hash-abc");
		token.setCreatedAt(LocalDateTime.now());
		token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
		token.setBruker(bruker);

		when(properties.getTokenSecret()).thenReturn("test-secret");
		when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
		when(passwordEncoder.encode("nyttpassord")).thenReturn("newHash");

		boolean result = passwordResetService.resetPassword("abc", "nyttpassord");

		assertTrue(result);
		assertEquals("newHash", bruker.getPassword());
		assertNotNull(token.getUsedAt());
		verify(brukerRepository).save(bruker);
		verify(tokenRepository, atLeastOnce()).save(token);
		verify(auditService).resetTokenUsed(bruker);
	}

	@Test
	void resetPassword_whenTokenMissing_returnsFalse() {
		when(properties.getTokenSecret()).thenReturn("test-secret");
		when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		boolean result = passwordResetService.resetPassword("mangler", "nyttpassord");

		assertFalse(result);
		verify(brukerRepository, never()).save(any(Bruker.class));
		verify(auditService).resetTokenInvalid("mangler");
	}

	@Test
	void requestPasswordReset_invalidatesOpenTokens() {
		when(properties.getTokenTtlMinutes()).thenReturn(30);
		when(properties.getTokenSecret()).thenReturn("test-secret");
		PasswordResetToken existingToken = new PasswordResetToken();
		existingToken.setTokenHash("old-hash");
		existingToken.setBruker(bruker);
		existingToken.setCreatedAt(LocalDateTime.now().minusMinutes(5));
		existingToken.setExpiresAt(LocalDateTime.now().plusMinutes(20));

		when(brukerRepository.findByEpost("test@example.com")).thenReturn(Optional.of(bruker));
		when(tokenRepository.findByBrukerAndUsedAtIsNull(bruker)).thenReturn(List.of(existingToken));

		passwordResetService.requestPasswordReset("test@example.com");

		assertNotNull(existingToken.getUsedAt());
		verify(tokenRepository).saveAll(anyList());
		verify(auditService).resetRequested(bruker);
	}
}
