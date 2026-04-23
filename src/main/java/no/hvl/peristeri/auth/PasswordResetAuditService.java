package no.hvl.peristeri.auth;

import lombok.extern.slf4j.Slf4j;
import no.hvl.peristeri.feature.bruker.Bruker;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PasswordResetAuditService {

	public void resetRequested(Bruker bruker) {
		log.info("password_reset_requested userId={} email={}", bruker.getId(), bruker.getEpost());
	}

	public void resetRateLimited(String epost, String ipAddress) {
		log.warn("password_reset_rate_limited email={} ip={}", epost, ipAddress);
	}

	public void resetTokenInvalid(String tokenPrefix) {
		log.warn("password_reset_invalid_token tokenPrefix={}", tokenPrefix);
	}

	public void resetTokenUsed(Bruker bruker) {
		log.info("password_reset_success userId={} email={}", bruker.getId(), bruker.getEpost());
	}

	public void resetRejected(String reason) {
		log.warn("password_reset_rejected reason={}", reason);
	}
}

