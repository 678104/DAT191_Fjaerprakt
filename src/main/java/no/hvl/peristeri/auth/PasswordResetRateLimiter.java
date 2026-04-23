package no.hvl.peristeri.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class PasswordResetRateLimiter {

	private final PasswordResetProperties properties;
	private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

	public boolean isAllowed(String epost, String ipAddress) {
		String normalizedEmail = normalize(epost);
		String normalizedIp = normalize(ipAddress);

		return isAllowedForKey("email:" + normalizedEmail)
				&& isAllowedForKey("ip:" + normalizedIp);
	}

	private synchronized boolean isAllowedForKey(String key) {
		int maxAttempts = properties.getRateLimitMaxAttempts();
		if (maxAttempts <= 0) {
			return true;
		}

		Instant now = Instant.now();
		Instant cutoff = now.minusSeconds(Math.max(1, properties.getRateLimitWindowMinutes()) * 60L);

		Deque<Instant> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
			attempts.removeFirst();
		}

		if (attempts.size() >= maxAttempts) {
			return false;
		}

		attempts.addLast(now);
		return true;
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		return value.trim().toLowerCase();
	}
}

