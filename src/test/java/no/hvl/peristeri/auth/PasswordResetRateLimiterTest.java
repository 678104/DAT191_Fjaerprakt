package no.hvl.peristeri.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetRateLimiterTest {

	private PasswordResetProperties properties;
	private PasswordResetRateLimiter rateLimiter;

	@BeforeEach
	void setup() {
		properties = new PasswordResetProperties();
		properties.setRateLimitMaxAttempts(2);
		properties.setRateLimitWindowMinutes(15);
		rateLimiter = new PasswordResetRateLimiter(properties);
	}

	@Test
	void isAllowed_shouldBlockAfterMaxAttemptsForSameEmailAndIp() {
		assertTrue(rateLimiter.isAllowed("test@example.com", "127.0.0.1"));
		assertTrue(rateLimiter.isAllowed("test@example.com", "127.0.0.1"));
		assertFalse(rateLimiter.isAllowed("test@example.com", "127.0.0.1"));
	}

	@Test
	void isAllowed_shouldTrackByEmailAndIpSeparately() {
		assertTrue(rateLimiter.isAllowed("test@example.com", "127.0.0.1"));
		assertTrue(rateLimiter.isAllowed("other@example.com", "127.0.0.2"));
		assertTrue(rateLimiter.isAllowed("other@example.com", "127.0.0.2"));
		assertFalse(rateLimiter.isAllowed("other@example.com", "127.0.0.2"));
	}
}

