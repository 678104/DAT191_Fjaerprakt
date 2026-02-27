package no.hvl.peristeri.config;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import no.hvl.peristeri.feature.bruker.Rolle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DefaultAdminAccountCreator implements CommandLineRunner {

	private final Logger logger = LoggerFactory.getLogger(DefaultAdminAccountCreator.class);

	private final PasswordEncoder passwordEncoder;
	private final BrukerService brukerService;


	@Value("${app.admin.email:}")
	private String email;

	@Value("${app.admin.name:}")
	private String name;

	@Value("${app.admin.password:}")
	private String password;

	@Override
	public void run(String... args) {
		if (email.isEmpty() || name.isEmpty() || password.isEmpty()) {
			logger.warn("""
						Admin account creation skipped. Please set app.admin.email, app.admin.name, and app.admin.password properties.
						This can be done with cmdline -Dapp.admin.email=my@email.com -Dapp.admin.name=MyName -Dapp.admin.password=MyPassword
						Or by setting the environment variables APP_ADMIN_EMAIL, APP_ADMIN_NAME, and APP_ADMIN_PASSWORD."""
			);
			return;
		}

		if (brukerService.findByEpost(email).isPresent()) {
			logger.info("Account with email {} already exists. Skipping creation.", email);
			return;
		}

		logger.info("Creating admin account with email: {}", email);

		Bruker admin = new Bruker(name, name, "", email, "", "", Rolle.ADMIN);
		setPassword(admin, password);
		brukerService.lagreBruker(admin);
		logger.info("Admin account created with email: {}", email);
	}

	private void setPassword(Bruker bruker, String nyttPassord) {
		bruker.setPassword(passwordEncoder.encode(nyttPassord));
	}
}
