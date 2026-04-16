package no.hvl.peristeri.feature.bruker;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BrukerService {
	List<Bruker> getBrukere();

	List<Bruker> finnBrukere(String sok);

	List<Bruker> finnBrukereForDommerAutocomplete(String sok, int maksAntall);

	Optional<Bruker> getBruker(String fornavn, String etternavn);

	/**
	 * Bruker repository save metode til å øagre en bruker<br>
	 * Denne kan både lage en ny og oppdatere eksisterende bruker i database hvis id finnes.
	 *
	 * @param bruker Brukeren som skal lagres
	 *
	 * @return Den lagrede brukeren for videre arbeid
	 */
	Bruker lagreBruker(Bruker bruker);

	List<Bruker> addAll(List<Bruker> brukere);

	Bruker hentBrukerMedId(Long id);

	Bruker oppdaterBrukerInfo(Long id, String fornavn, String etternavn, String telefon, String epost,
	                          String adresse, String postnummer, String poststed, String forening);

	void refreshUserAuthentication(Bruker bruker);

	/**
	 * Changes a user's password after validating the current password
	 *
	 * @param bruker          The user whose password should be changed
	 * @param currentPassword The current password (for verification)
	 * @param newPassword     The new password to set
	 *
	 * @return true if password was changed successfully, false otherwise
	 */
	@Transactional
	boolean endrePassord(Bruker bruker, String currentPassword, String newPassword);

	/**
	 * Setter en bruker sitt passord, ved å bruke password encoder, og lagrer bruker i database.
	 *
	 * @param bruker  Brukeren som skal få passord lagret
	 * @param passord Passordet som skal lagres
	 *
	 * @return Den lagrede brukeren
	 *
	 * @implNote Denne skal kun brukes ved registrering, bruk {@link BrukerService#endrePassord(Bruker, String, String)} ved endring av passord
	 */
	Bruker lagreBrukerMedPassord(Bruker bruker, String passord);

	boolean sjekkOmEpostErBrukt(@Email @NotBlank String epost);

	Optional<Bruker> findByEpost(String email);

	List<Bruker> hentBrukereMedRolle(Rolle rolle);

	Bruker leggTilRolle(Long brukerId, Rolle rolle);

	Bruker fjernRolle(Long brukerId, Rolle rolle);

	boolean harRolle(Long brukerId, Rolle rolle);
}
