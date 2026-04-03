package no.hvl.peristeri.feature.bruker;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class BrukerServiceImpl implements BrukerService {

	private final BrukerRepository                   brukerRepository;
	private final PasswordEncoder                    passwordEncoder;

	@Override
	public List<Bruker> getBrukere() {
		return brukerRepository.findAll();
	}

	@Override
	public List<Bruker> finnBrukere(String sok) {
		if (sok == null || sok.isBlank()) {
			return getBrukere();
		}

		String filter = sok.trim();
		if (filter.isEmpty()) {
			return getBrukere();
		}

		return brukerRepository.findByFornavnStartingWithIgnoreCaseOrEtternavnStartingWithIgnoreCaseOrEpostStartingWithIgnoreCase(
				filter, filter, filter);
	}

	@Override
	public Optional<Bruker> getBruker(String fornavn, String etternavn) {
		if (fornavn == null) {
			throw new InvalidParameterException("fornavn", "cannot be null");
		}
		if (etternavn == null) {
			throw new InvalidParameterException("etternavn", "cannot be null");
		}
		return brukerRepository.findFirstByFornavnAndEtternavn(fornavn, etternavn);
	}

	/**
	 * Bruker repository save metode til å øagre en bruker<br>
	 * Denne kan både lage en ny og oppdatere eksisterende bruker i database hvis id finnes.
	 *
	 * @param bruker Brukeren som skal lagres
	 *
	 * @return Den lagrede brukeren for videre arbeid
	 * @throws InvalidParameterException hvis bruker er null
	 */
	@Override
	public Bruker lagreBruker(Bruker bruker) {
		if (bruker == null) {
			throw new InvalidParameterException("bruker", "cannot be null");
		}
		return brukerRepository.save(bruker);
	}

	@Override
	public List<Bruker> addAll(List<Bruker> brukere) {
		if (brukere == null) {
			throw new InvalidParameterException("brukere", "cannot be null");
		}
		return brukerRepository.saveAll(brukere);
	}

	@Override
	public Bruker hentBrukerMedId(Long id) {
		if (id == null) {
			throw new InvalidParameterException("id", "cannot be null");
		}
		return brukerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Bruker", id));
	}

	@Override
	public Bruker oppdaterBrukerInfo(Long id, String fornavn, String etternavn, String telefon, String epost,
	                                 String adresse, String postnummer, String poststed, String forening) {
		if (id == null) {
			throw new InvalidParameterException("id", "cannot be null");
		}

		Bruker bruker = hentBrukerMedId(id);
		bruker.setFornavn(fornavn);
		bruker.setEtternavn(etternavn);
		bruker.setTelefon(telefon);
		bruker.setEpost(epost);
		bruker.setAdresse(adresse);
		bruker.setPostnummer(postnummer);
		bruker.setPoststed(poststed);
		bruker.setForening(forening);
		brukerRepository.save(bruker);
		return bruker;
	}

	@Override
	public void refreshUserAuthentication(Bruker bruker) {
		if (bruker == null) {
			throw new InvalidParameterException("bruker", "cannot be null");
		}

		// Create a new authentication with the updated user
		Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

		// Create updated authentication with same credentials but new user details
		Authentication newAuth = new UsernamePasswordAuthenticationToken(
				bruker,
				currentAuth.getCredentials(),
				bruker.getAuthorities());

		// Update the security context
		SecurityContextHolder.getContext().setAuthentication(newAuth);
	}

	/**
	 * Changes a user's password after validating the current password
	 *
	 * @param bruker          The user whose password should be changed
	 * @param currentPassword The current password (for verification)
	 * @param newPassword     The new password to set
	 *
	 * @return true if password was changed successfully, false otherwise
	 * @throws InvalidParameterException if any parameter is null
	 */
	@Transactional
	@Override
	public boolean endrePassord(Bruker bruker, String currentPassword, String newPassword) {
		if (bruker == null) {
			throw new InvalidParameterException("bruker", "cannot be null");
		}
		if (currentPassword == null) {
			throw new InvalidParameterException("currentPassword", "cannot be null");
		}
		if (newPassword == null) {
			throw new InvalidParameterException("newPassword", "cannot be null");
		}

		// Verify current password
		if (!passwordEncoder.matches(currentPassword, bruker.getPassword())) {
			return false;
		}

		// Encode and set new password
		bruker.setPassword(passwordEncoder.encode(newPassword));
		brukerRepository.save(bruker);
		return true;
	}

	/**
	 * Setter en bruker sitt passord, ved å bruke password encoder, og lagrer bruker i database.
	 *
	 * @param bruker  Brukeren som skal få passord lagret
	 * @param passord Passordet som skal lagres
	 *
	 * @return Den lagrede brukeren
	 * @throws InvalidParameterException hvis bruker eller passord er null
	 *
	 * @implNote Denne skal kun brukes ved registrering, bruk {@link BrukerServiceImpl#endrePassord(Bruker, String, String)} ved endring av passord
	 */
	@Override
	public Bruker lagreBrukerMedPassord(Bruker bruker, String passord) {
		if (bruker == null) {
			throw new InvalidParameterException("bruker", "cannot be null");
		}
		if (passord == null) {
			throw new InvalidParameterException("passord", "cannot be null");
		}

		bruker.setPassword(passwordEncoder.encode(passord));
		return brukerRepository.save(bruker);
	}

	@Override
	public boolean sjekkOmEpostErBrukt(@Email @NotBlank String epost) {
		if (epost == null) {
			throw new InvalidParameterException("epost", "cannot be null");
		}
		return brukerRepository.findByEpost(epost).isPresent();
	}

	@Override
	public Optional<Bruker> findByEpost(String email) {
		if (email == null) {
			throw new InvalidParameterException("email", "cannot be null");
		}
		return brukerRepository.findByEpost(email);
	}
}
