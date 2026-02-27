package no.hvl.peristeri.feature.utstilling;

import no.hvl.peristeri.common.DateRange;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import no.hvl.peristeri.feature.due.DueRepository;
import no.hvl.peristeri.feature.due.DueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtstillingServiceImplTest {

    @Mock
    private UtstillingRepository utstillingRepository;

    @Mock
    private DueRepository dueRepository;

    @Mock
    private DueService dueService;

    @InjectMocks
    private UtstillingServiceImpl utstillingService;

    private Utstilling testUtstilling;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        testUtstilling = new Utstilling();
        testUtstilling.setId(1L);
        testUtstilling.setTittel("Test Utstilling");
        testUtstilling.setArrangoer("Test Arrangoer");
        testUtstilling.setAdresse("Test Adresse");
        testUtstilling.setPostnummer("1234");
        testUtstilling.setPoststed("Test Poststed");
        testUtstilling.setBeskrivelse("Test Beskrivelse");
        testUtstilling.setDatoRange(new DateRange(today.plusDays(10), today.plusDays(12)));
        testUtstilling.setPaameldingsFrist(today.plusDays(5));
        testUtstilling.setPaameldingAApnet(true);
        testUtstilling.setDuePris(BigDecimal.valueOf(100.0));
        testUtstilling.setRaseSortering("Norsk Tomler|Dansk Tomler|Tysk Modeneser");
        testUtstilling.setHarBurnumre(false);
        testUtstilling.setAktiv(false);
    }

    @Test
    void finnAktivUtstilling_whenActiveExhibitionExists_shouldReturnIt() {
        // Arrange
        when(utstillingRepository.finnAktivUtstilling()).thenReturn(Optional.of(testUtstilling));

        // Act
        Utstilling result = utstillingService.finnAktivUtstilling();

        // Assert
        assertEquals(testUtstilling, result);
        verify(utstillingRepository).finnAktivUtstilling();
    }

    @Test
    void finnAktivUtstilling_whenNoActiveExhibition_shouldReturnNull() {
        // Arrange
        when(utstillingRepository.finnAktivUtstilling()).thenReturn(Optional.empty());

        // Act
        Utstilling result = utstillingService.finnAktivUtstilling();

        // Assert
        assertNull(result);
        verify(utstillingRepository).finnAktivUtstilling();
    }

    @Test
    void setAktivUtstilling_withExistingId_shouldSetAsActiveAndReturnIt() {
        // Arrange
        when(utstillingRepository.findById(1L)).thenReturn(Optional.of(testUtstilling));
        when(utstillingRepository.save(testUtstilling)).thenReturn(testUtstilling);

        // Act
        Utstilling result = utstillingService.setAktivUtstilling(1L);

        // Assert
        assertTrue(result.getAktiv());
        assertEquals(testUtstilling, result);
        verify(utstillingRepository).deaktiverAktiveUtstillinger();
        verify(utstillingRepository).findById(1L);
        verify(utstillingRepository).save(testUtstilling);
    }

    @Test
    void setAktivUtstilling_withNonExistingId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(utstillingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> utstillingService.setAktivUtstilling(999L)
        );
        assertEquals("Utstilling with id 999 not found", exception.getMessage());
        verify(utstillingRepository).findById(999L);
        verify(utstillingRepository, never()).save(any());
    }

    @Test
    void fjernAktivUtstilling_shouldDeactivateAllExhibitions() {
        // Act
        utstillingService.fjernAktivUtstilling();

        // Assert
        verify(utstillingRepository).deaktiverAktiveUtstillinger();
    }

    @Test
    void leggTilUtstilling_shouldSaveAndReturnExhibition() {
        // Arrange
        when(utstillingRepository.save(testUtstilling)).thenReturn(testUtstilling);

        // Act
        Utstilling result = utstillingService.leggTilUtstilling(testUtstilling);

        // Assert
        assertEquals(testUtstilling, result);
        verify(utstillingRepository).save(testUtstilling);
    }

    @Test
    void oppdaterUtstilling_withExistingId_shouldUpdateAndReturnExhibition() {
        // Arrange
        Utstilling existingUtstilling = new Utstilling();
        existingUtstilling.setId(1L);
        existingUtstilling.setTittel("Old Title");
        existingUtstilling.setArrangoer("Old Arrangoer");

        when(utstillingRepository.findById(1L)).thenReturn(Optional.of(existingUtstilling));
        when(utstillingRepository.save(existingUtstilling)).thenReturn(existingUtstilling);

        // Act
        Utstilling result = utstillingService.oppdaterUtstilling(1L, testUtstilling);

        // Assert
        assertEquals("Test Utstilling", result.getTittel());
        assertEquals("Test Arrangoer", result.getArrangoer());
        verify(utstillingRepository).findById(1L);
        verify(utstillingRepository).save(existingUtstilling);
    }

    @Test
    void oppdaterUtstilling_withIdMismatch_shouldThrowException() {
        // Arrange
        // testUtstilling has ID 1, but we're trying to update ID 999

        // Act & Assert
        Exception exception = assertThrows(
                Exception.class,
                () -> utstillingService.oppdaterUtstilling(999L, testUtstilling)
        );
        assertTrue(exception.getMessage().contains("ID mismatch"));
        verify(utstillingRepository, never()).save(any());
    }

    @Test
    void finnUtstillingMedId_withExistingId_shouldReturnExhibition() {
        // Arrange
        when(utstillingRepository.findById(1L)).thenReturn(Optional.of(testUtstilling));

        // Act
        Utstilling result = utstillingService.finnUtstillingMedId(1L);

        // Assert
        assertEquals(testUtstilling, result);
        verify(utstillingRepository).findById(1L);
    }

    @Test
    void finnUtstillingMedId_withNonExistingId_shouldThrowException() {
        // Arrange
        when(utstillingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> utstillingService.finnUtstillingMedId(999L)
        );
        assertEquals("Utstilling with id 999 not found", exception.getMessage());
        verify(utstillingRepository).findById(999L);
    }

    @Test
    void addAll_shouldSaveAndReturnExhibitions() {
        // Arrange
        List<Utstilling> utstillinger = Arrays.asList(testUtstilling, new Utstilling());
        when(utstillingRepository.saveAll(utstillinger)).thenReturn(utstillinger);

        // Act
        List<Utstilling> result = utstillingService.addAll(utstillinger);

        // Assert
        assertEquals(utstillinger, result);
        verify(utstillingRepository).saveAll(utstillinger);
    }

    @Test
    void hentAlleUtstillinger_shouldReturnAllExhibitions() {
        // Arrange
        List<Utstilling> utstillinger = Arrays.asList(testUtstilling, new Utstilling());
        when(utstillingRepository.findAll(any(Sort.class))).thenReturn(utstillinger);

        // Act
        List<Utstilling> result = utstillingService.hentAlleUtstillinger();

        // Assert
        assertEquals(utstillinger, result);
        verify(utstillingRepository).findAll(any(Sort.class));
    }

    @Test
    void finnIkkeTidligereUtstillinger_shouldReturnUpcomingExhibitions() {
        // Arrange
        List<Utstilling> utstillinger = Arrays.asList(testUtstilling, new Utstilling());
        when(utstillingRepository.finnUtstillingerSomAvslutterEtterGittDato(any())).thenReturn(utstillinger);

        // Act
        List<Utstilling> result = utstillingService.finnIkkeTidligereUtstillinger();

        // Assert
        assertEquals(utstillinger, result);
        verify(utstillingRepository).finnUtstillingerSomAvslutterEtterGittDato(any());
    }

    @Test
    void finnUtstillingerMedMulighetForPaamelding_shouldReturnExhibitionsWithOpenRegistration() {
        // Arrange
        List<Utstilling> utstillinger = Arrays.asList(testUtstilling, new Utstilling());
        when(utstillingRepository.finnUtstillingerMedAApenPaameldingSortertEtterStartdato()).thenReturn(utstillinger);

        // Act
        List<Utstilling> result = utstillingService.finnUtstillingerMedMulighetForPaamelding();

        // Assert
        assertEquals(utstillinger, result);
        verify(utstillingRepository).finnUtstillingerMedAApenPaameldingSortertEtterStartdato();
    }

    @Test
    void finnKommendeUtstillinger_shouldReturnUpcomingExhibitions() {
        // Arrange
        List<Utstilling> utstillinger = Arrays.asList(testUtstilling, new Utstilling());
        when(utstillingRepository.finnUtstillingerSomStarterEtterGittDato(any())).thenReturn(utstillinger);

        // Act
        List<Utstilling> result = utstillingService.finnKommendeUtstillinger();

        // Assert
        assertEquals(utstillinger, result);
        verify(utstillingRepository).finnUtstillingerSomStarterEtterGittDato(any());
    }

    @Test
    void finnTidligereUtstillinger_shouldReturnPastExhibitions() {
        // Arrange
        List<Utstilling> utstillinger = Arrays.asList(testUtstilling, new Utstilling());
        when(utstillingRepository.finnUtstillingerSomEnderFoerGittDato(any())).thenReturn(utstillinger);

        // Act
        List<Utstilling> result = utstillingService.finnTidligereUtstillinger();

        // Assert
        assertEquals(utstillinger, result);
        verify(utstillingRepository).finnUtstillingerSomEnderFoerGittDato(any());
    }

    @Test
    void oppdaterSorterteRaser_withExistingId_shouldUpdateAndReturnExhibition() {
        // Arrange
        when(utstillingRepository.findById(1L)).thenReturn(Optional.of(testUtstilling));
        when(utstillingRepository.save(testUtstilling)).thenReturn(testUtstilling);

        // Act
        Utstilling result = utstillingService.oppdaterSorterteRaser(1L, "Dansk Tomler|Norsk Tomler|Tysk Modeneser");

        // Assert
        assertEquals("Dansk Tomler|Norsk Tomler|Tysk Modeneser", result.getRaseSortering());
        verify(utstillingRepository).findById(1L);
        verify(utstillingRepository).save(testUtstilling);
    }

    @Test
    void oppdaterSorterteRaser_withNonExistingId_shouldThrowException() {
        // Arrange
        when(utstillingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> utstillingService.oppdaterSorterteRaser(999L, "Dansk Tomler|Norsk Tomler|Tysk Modeneser")
        );
        assertEquals("Utstilling with id 999 not found", exception.getMessage());
        verify(utstillingRepository).findById(999L);
        verify(utstillingRepository, never()).save(any());
    }
}
