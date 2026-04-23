package no.hvl.peristeri.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@RequiredArgsConstructor
@Service
public class PasswordResetMailService {

	private static final Logger logger = LoggerFactory.getLogger(PasswordResetMailService.class);

	private final JavaMailSender mailSender;
	private final SpringTemplateEngine templateEngine;
	private final PasswordResetProperties properties;
	private final Environment environment;

	public String createResetUrl(String token) {
		String baseUrl = properties.getBaseUrl();
		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl + "/reset-passord?token=" + token;
	}

	public void sendPasswordResetEmail(String recipient, String resetUrl) {
		if (!properties.isMailEnabled()) {
			if (properties.isLogResetLinkInDev() && environment.acceptsProfiles(Profiles.of("dev"))) {
				logger.info("[DEV] Passord reset-lenke for {}: {}", recipient, resetUrl);
			}
			return;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			Context context = new Context();
			context.setVariable("resetUrl", resetUrl);
			context.setVariable("expiresInMinutes", properties.getTokenTtlMinutes());

			String htmlContent = templateEngine.process("mail/password-reset-email", context);

			helper.setTo(recipient);
			helper.setFrom(properties.getMailFrom());
			helper.setSubject("Tilbakestill passord");
			helper.setText(htmlContent, true);

			mailSender.send(message);
		} catch (MessagingException | MailException ex) {
			logger.warn("Klarte ikke sende passord-reset e-post til {}. Feil: {}", recipient, ex.getMessage());
		}
	}
}


