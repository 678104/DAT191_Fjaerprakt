package no.hvl.peristeri.feature.bruker;

import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrukerServiceImplTest {

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BrukerServiceImpl brukerService;

    private Bruker testBruker;

    @BeforeEach
    void setUp() {
        testBruker = new Bruker();
        testBruker.setId(1L);
        testBruker.setFornavn("Test");
        testBruker.setEtternavn("Bruker");
        testBruker.setEpost("test@example.com");
        testBruker.setPassword("encodedPassword");
        testBruker.leggTilRolle(Rolle.UTSTILLER);
    }

    @Test
    void getBrukere_shouldReturnAllUsers() {
        // Arrange
        List<Bruker> expectedBrukere = Arrays.asList(testBruker, new Bruker());
        when(brukerRepository.findAll()).thenReturn(expectedBrukere);

        // Act
        List<Bruker> actualBrukere = brukerService.getBrukere();

        // Assert
        assertEquals(expectedBrukere, actualBrukere);
        verify(brukerRepository).findAll();
    }

    @Test
    void finnBrukere_withSearchTerm_shouldSearchForFornavnEtternavnAndEpost() {
        // Arrange
        List<Bruker> expectedBrukere = List.of(testBruker);
            when(brukerRepository.findByFornavnContainingIgnoreCaseOrEtternavnContainingIgnoreCaseOrEpostStartingWithIgnoreCase(
                "Test", "Test", "Test")).thenReturn(expectedBrukere);

        // Act
        List<Bruker> actualBrukere = brukerService.finnBrukere("Test");

        // Assert
        assertEquals(expectedBrukere, actualBrukere);
            verify(brukerRepository).findByFornavnContainingIgnoreCaseOrEtternavnContainingIgnoreCaseOrEpostStartingWithIgnoreCase(
                "Test", "Test", "Test");
    }

    @Test
    void finnBrukere_withBlankSearch_shouldReturnAllUsers() {
        // Arrange
        List<Bruker> expectedBrukere = Arrays.asList(testBruker, new Bruker());
        when(brukerRepository.findAll()).thenReturn(expectedBrukere);

        // Act
        List<Bruker> actualBrukere = brukerService.finnBrukere("   ");

        // Assert
        assertEquals(expectedBrukere, actualBrukere);
        verify(brukerRepository).findAll();
            verify(brukerRepository, never()).findByFornavnContainingIgnoreCaseOrEtternavnContainingIgnoreCaseOrEpostStartingWithIgnoreCase(
                any(), any(), any());
    }

    @Test
    void getBruker_withValidNames_shouldReturnUser() {
        // Arrange
        when(brukerRepository.findFirstByFornavnAndEtternavn("Test", "Bruker"))
                .thenReturn(Optional.of(testBruker));

        // Act
        Optional<Bruker> result = brukerService.getBruker("Test", "Bruker");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testBruker, result.get());
        verify(brukerRepository).findFirstByFornavnAndEtternavn("Test", "Bruker");
    }

    @Test
    void getBruker_withNullFornavn_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.getBruker(null, "Bruker")
        );
        assertEquals("Invalid parameter 'fornavn': cannot be null", exception.getMessage());
    }

    @Test
    void getBruker_withNullEtternavn_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.getBruker("Test", null)
        );
        assertEquals("Invalid parameter 'etternavn': cannot be null", exception.getMessage());
    }

    @Test
    void lagreBruker_withValidBruker_shouldSaveAndReturnBruker() {
        // Arrange
        when(brukerRepository.save(testBruker)).thenReturn(testBruker);

        // Act
        Bruker result = brukerService.lagreBruker(testBruker);

        // Assert
        assertEquals(testBruker, result);
        verify(brukerRepository).save(testBruker);
    }

    @Test
    void lagreBruker_withNullBruker_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.lagreBruker(null)
        );
        assertEquals("Invalid parameter 'bruker': cannot be null", exception.getMessage());
    }

    @Test
    void addAll_withValidBrukere_shouldSaveAllAndReturnBrukere() {
        // Arrange
        List<Bruker> brukere = Arrays.asList(testBruker, new Bruker());
        when(brukerRepository.saveAll(brukere)).thenReturn(brukere);

        // Act
        List<Bruker> result = brukerService.addAll(brukere);

        // Assert
        assertEquals(brukere, result);
        verify(brukerRepository).saveAll(brukere);
    }

    @Test
    void addAll_withNullBrukere_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.addAll(null)
        );
        assertEquals("Invalid parameter 'brukere': cannot be null", exception.getMessage());
    }

    @Test
    void hentBrukerMedId_withExistingId_shouldReturnBruker() {
        // Arrange
        when(brukerRepository.findById(1L)).thenReturn(Optional.of(testBruker));

        // Act
        Bruker result = brukerService.hentBrukerMedId(1L);

        // Assert
        assertEquals(testBruker, result);
        verify(brukerRepository).findById(1L);
    }

    @Test
    void hentBrukerMedId_withNonExistingId_shouldThrowException() {
        // Arrange
        when(brukerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> brukerService.hentBrukerMedId(999L)
        );
        assertEquals("Bruker with id 999 not found", exception.getMessage());
    }

    @Test
    void hentBrukerMedId_withNullId_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.hentBrukerMedId(null)
        );
        assertEquals("Invalid parameter 'id': cannot be null", exception.getMessage());
    }

    @Test
    void oppdaterBrukerInfo_withValidData_shouldUpdateAndReturnBruker() {
        // Arrange
        when(brukerRepository.findById(1L)).thenReturn(Optional.of(testBruker));

        // Act
        Bruker result = brukerService.oppdaterBrukerInfo(
                1L, "Updated", "User", "12345678", "updated@example.com",
                "New Address", "12345", "New City", "New Club"
        );

        // Assert
        assertEquals("Updated", result.getFornavn());
        assertEquals("User", result.getEtternavn());
        assertEquals("12345678", result.getTelefon());
        assertEquals("updated@example.com", result.getEpost());
        assertEquals("New Address", result.getAdresse());
        assertEquals("12345", result.getPostnummer());
        assertEquals("New City", result.getPoststed());
        assertEquals("New Club", result.getForening());
        verify(brukerRepository).save(testBruker);
    }

    @Test
    void oppdaterBrukerInfo_withNullId_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.oppdaterBrukerInfo(
                        null, "Updated", "User", "12345678", "updated@example.com",
                        "New Address", "12345", "New City", "New Club"
                )
        );
        assertEquals("Invalid parameter 'id': cannot be null", exception.getMessage());
    }

    @Test
    void refreshUserAuthentication_withValidBruker_shouldUpdateSecurityContext() {
        // Arrange
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // Act
        brukerService.refreshUserAuthentication(testBruker);

        // Assert
        verify(securityContext).setAuthentication(any());
    }

    @Test
    void refreshUserAuthentication_withNullBruker_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.refreshUserAuthentication(null)
        );
        assertEquals("Invalid parameter 'bruker': cannot be null", exception.getMessage());
    }

    @Test
    void endrePassord_withCorrectCurrentPassword_shouldUpdatePasswordAndReturnTrue() {
        // Arrange
        when(passwordEncoder.matches("currentPassword", testBruker.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // Act
        boolean result = brukerService.endrePassord(testBruker, "currentPassword", "newPassword");

        // Assert
        assertTrue(result);
        assertEquals("encodedNewPassword", testBruker.getPassword());
        verify(brukerRepository).save(testBruker);
    }

    @Test
    void endrePassord_withIncorrectCurrentPassword_shouldReturnFalse() {
        // Arrange
        when(passwordEncoder.matches("wrongPassword", testBruker.getPassword())).thenReturn(false);

        // Act
        boolean result = brukerService.endrePassord(testBruker, "wrongPassword", "newPassword");

        // Assert
        assertFalse(result);
        assertEquals("encodedPassword", testBruker.getPassword());
        verify(brukerRepository, never()).save(any());
    }

    @Test
    void endrePassord_withNullParameters_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception1 = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.endrePassord(null, "current", "new")
        );
        assertEquals("Invalid parameter 'bruker': cannot be null", exception1.getMessage());

        InvalidParameterException exception2 = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.endrePassord(testBruker, null, "new")
        );
        assertEquals("Invalid parameter 'currentPassword': cannot be null", exception2.getMessage());

        InvalidParameterException exception3 = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.endrePassord(testBruker, "current", null)
        );
        assertEquals("Invalid parameter 'newPassword': cannot be null", exception3.getMessage());
    }

    @Test
    void lagreBrukerMedPassord_withValidData_shouldEncodePasswordAndSaveBruker() {
        // Arrange
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(brukerRepository.save(testBruker)).thenReturn(testBruker);

        // Act
        Bruker result = brukerService.lagreBrukerMedPassord(testBruker, "password");

        // Assert
        assertEquals(testBruker, result);
        assertEquals("encodedPassword", testBruker.getPassword());
        verify(passwordEncoder).encode("password");
        verify(brukerRepository).save(testBruker);
    }

    @Test
    void lagreBrukerMedPassord_withNullParameters_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception1 = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.lagreBrukerMedPassord(null, "password")
        );
        assertEquals("Invalid parameter 'bruker': cannot be null", exception1.getMessage());

        InvalidParameterException exception2 = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.lagreBrukerMedPassord(testBruker, null)
        );
        assertEquals("Invalid parameter 'passord': cannot be null", exception2.getMessage());
    }

    @Test
    void sjekkOmEpostErBrukt_withExistingEmail_shouldReturnTrue() {
        // Arrange
        when(brukerRepository.findByEpost("test@example.com")).thenReturn(Optional.of(testBruker));

        // Act
        boolean result = brukerService.sjekkOmEpostErBrukt("test@example.com");

        // Assert
        assertTrue(result);
        verify(brukerRepository).findByEpost("test@example.com");
    }

    @Test
    void sjekkOmEpostErBrukt_withNonExistingEmail_shouldReturnFalse() {
        // Arrange
        when(brukerRepository.findByEpost("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        boolean result = brukerService.sjekkOmEpostErBrukt("nonexistent@example.com");

        // Assert
        assertFalse(result);
        verify(brukerRepository).findByEpost("nonexistent@example.com");
    }

    @Test
    void sjekkOmEpostErBrukt_withNullEmail_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.sjekkOmEpostErBrukt(null)
        );
        assertEquals("Invalid parameter 'epost': cannot be null", exception.getMessage());
    }

    @Test
    void findByEpost_withExistingEmail_shouldReturnBruker() {
        // Arrange
        when(brukerRepository.findByEpost("test@example.com")).thenReturn(Optional.of(testBruker));

        // Act
        Optional<Bruker> result = brukerService.findByEpost("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testBruker, result.get());
        verify(brukerRepository).findByEpost("test@example.com");
    }

    @Test
    void findByEpost_withNonExistingEmail_shouldReturnEmptyOptional() {
        // Arrange
        when(brukerRepository.findByEpost("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<Bruker> result = brukerService.findByEpost("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
        verify(brukerRepository).findByEpost("nonexistent@example.com");
    }

    @Test
    void findByEpost_withNullEmail_shouldThrowException() {
        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> brukerService.findByEpost(null)
        );
        assertEquals("Invalid parameter 'email': cannot be null", exception.getMessage());
    }
}
