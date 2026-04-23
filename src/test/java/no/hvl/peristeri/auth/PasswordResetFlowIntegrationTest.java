package no.hvl.peristeri.auth;

import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BrukerRepository brukerRepository;

	@Autowired
	private PasswordResetService passwordResetService;

	@Autowired
	private PasswordResetTokenRepository tokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setup() {
		tokenRepository.deleteAll();
		brukerRepository.deleteAll();
	}

	@Test
	void forgotPasswordRequest_shouldStoreHashedTokenAndRedirect() throws Exception {
		Bruker bruker = new Bruker();
		bruker.setFornavn("Test");
		bruker.setEtternavn("Bruker");
		bruker.setEpost("test@example.com");
		bruker.setPassword(passwordEncoder.encode("hemlig"));
		brukerRepository.save(bruker);

		mockMvc.perform(MockMvcRequestBuilders.post("/glemt-passord")
				.with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("epost", "test@example.com"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/glemt-passord?sent=1"));

		PasswordResetToken storedToken = tokenRepository.findAll().getFirst();
		assertNotNull(storedToken.getTokenHash());
		assertEquals(64, storedToken.getTokenHash().length());
		assertNotEquals("test@example.com", storedToken.getTokenHash());
	}

	@Test
	void resetPasswordFlow_shouldUpdatePasswordAndMarkTokenUsed() throws Exception {
		Bruker bruker = new Bruker();
		bruker.setFornavn("Test");
		bruker.setEtternavn("Bruker");
		bruker.setEpost("test@example.com");
		bruker.setPassword(passwordEncoder.encode("gammelt"));
		brukerRepository.save(bruker);

		String rawToken = passwordResetService.requestPasswordReset("test@example.com").orElseThrow();

		mockMvc.perform(MockMvcRequestBuilders.get("/reset-passord")
				.param("token", rawToken))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/reset-password"))
				.andExpect(model().attribute("token", rawToken));

		mockMvc.perform(MockMvcRequestBuilders.post("/reset-passord")
				.with(csrf())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("token", rawToken)
				.param("passord", "nyttpassord")
				.param("bekreftPassord", "nyttpassord"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?resetSuccess=1"));

		Bruker oppdatertBruker = brukerRepository.findByEpost("test@example.com").orElseThrow();
		assertTrue(passwordEncoder.matches("nyttpassord", oppdatertBruker.getPassword()));

		PasswordResetToken token = tokenRepository.findAll().getFirst();
		assertNotNull(token.getUsedAt());
	}

	@Test
	void resetPasswordWithInvalidToken_shouldShowError() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/reset-passord")
				.param("token", "ugyldig-token"))
				.andExpect(status().isOk())
				.andExpect(view().name("auth/reset-password"))
				.andExpect(model().attribute("tokenInvalid", true));
	}
}


