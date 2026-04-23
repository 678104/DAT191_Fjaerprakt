package no.hvl.peristeri.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

	private String baseUrl = "http://localhost:8080";
	private String mailFrom = "no-reply@peristeri.local";
	private String tokenSecret = "change-me-in-production";
	private boolean mailEnabled = false;
	private boolean logResetLinkInDev = true;
	private int tokenTtlMinutes = 30;
	private int rateLimitMaxAttempts = 3;
	private int rateLimitWindowMinutes = 15;
}

