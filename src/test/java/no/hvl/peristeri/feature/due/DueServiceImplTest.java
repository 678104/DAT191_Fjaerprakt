package no.hvl.peristeri.feature.due;

import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueServiceImplTest {

    @Mock
    private DueRepository dueRepository;

    @Mock
    private DueLookupService dueLookupService;

    @InjectMocks
    private DueServiceImpl dueService;

    private Due testDue;
    private Rase testRase;
    private Farge testFarge;
    private Variant testVariant;
    private Long testUtstillingId;
    private List<Long> testDueIdList;

    @BeforeEach
    void setUp() {
        testDue = new Due();
        testDue.setId(1L);
        testRase = new Rase(1L, "Norsk Tomler", "Tomler");
        testFarge = new Farge(1L, "Rød");
        testVariant = new Variant(1L, "Standard");
        testDue.setRaseLookup(testRase);
        testDue.setFargeLookup(testFarge);
        testDue.setVariantLookup(testVariant);
        testDue.setKjonn(true); // Handue
        testDue.setAlder(true); // Eldre due
        testDue.setLopenummer("123");
        testDue.setAarstall("2023");
        testDue.setBurnummer(101);

        testUtstillingId = 1L;
        testDueIdList = Arrays.asList(1L, 2L, 3L);
    }

    @Test
    void leggTilDue_shouldSaveAndReturnDue() {
        // Arrange
        when(dueRepository.save(testDue)).thenReturn(testDue);

        // Act
        Due result = dueService.leggTilDue(testDue);

        // Assert
        assertEquals(testDue, result);
        verify(dueRepository).save(testDue);
    }

    @Test
    void oppdaterRingnummer_withExistingId_shouldUpdateAndReturnDue() {
        // Arrange
        when(dueRepository.findById(1L)).thenReturn(Optional.of(testDue));
        when(dueRepository.save(testDue)).thenReturn(testDue);

        // Act
        Due result = dueService.oppdaterRingnummer(1L, "456", "2024");

        // Assert
        assertEquals("456", result.getLopenummer());
        assertEquals("2024", result.getAarstall());
        verify(dueRepository).findById(1L);
        verify(dueRepository).save(testDue);
    }

    @Test
    void oppdaterRingnummer_withNonExistingId_shouldThrowException() {
        // Arrange
        when(dueRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> dueService.oppdaterRingnummer(999L, "456", "2024")
        );
        assertEquals("Due with id 999 not found", exception.getMessage());
        verify(dueRepository).findById(999L);
        verify(dueRepository, never()).save(any());
    }

    @Test
    void hentRaserPaameldtUtstilling_shouldReturnRases() {
        // Arrange
        List<String> expectedRases = Arrays.asList("Norsk Tomler", "Dansk Tomler", "Tysk Modeneser");
        when(dueRepository.hentRaserPaameldtUtstilling(testUtstillingId)).thenReturn(expectedRases);

        // Act
        List<String> result = dueService.hentRaserPaameldtUtstilling(testUtstillingId);

        // Assert
        assertEquals(expectedRases, result);
        verify(dueRepository).hentRaserPaameldtUtstilling(testUtstillingId);
    }

    @Test
    void finnAlleDuerPaameldtUTstilling_shouldReturnDuesSortedByBurnummer() {
        // Arrange
        List<Due> expectedDues = Arrays.asList(testDue, new Due());
        when(dueRepository.findByPaamelding_Utstilling_IdOrderByBurnummerAsc(testUtstillingId)).thenReturn(expectedDues);

        // Act
        List<Due> result = dueService.finnAlleDuerPaameldtUTstilling(testUtstillingId);

        // Assert
        assertEquals(expectedDues, result);
        verify(dueRepository).findByPaamelding_Utstilling_IdOrderByBurnummerAsc(testUtstillingId);
    }

    @Test
    void findAllSortedByCustomOrder_shouldReturnDuesSortedByCustomOrder() {
        // Arrange
        Due due1 = new Due();
        due1.setRaseLookup(new Rase(10L, "Norsk Tomler", "Tomler"));
        
        Due due2 = new Due();
        due2.setRaseLookup(new Rase(11L, "Dansk Tomler", "Tomler"));
        
        Due due3 = new Due();
        due3.setRaseLookup(new Rase(12L, "Tysk Modeneser", "Modeneser"));
        
        List<Due> duesFromRepo = Arrays.asList(due1, due2, due3);
        List<String> customOrder = Arrays.asList(
                "Dansk Tomler (Tomler)",
                "Tysk Modeneser (Modeneser)",
                "Norsk Tomler (Tomler)"
        );
        
        when(dueRepository.findByPaamelding_Utstilling_Id(testUtstillingId)).thenReturn(duesFromRepo);

        // Act
        List<Due> result = dueService.findAllSortedByCustomOrder(customOrder, testUtstillingId);

        // Assert
        assertEquals(3, result.size());
        assertEquals("Dansk Tomler (Tomler)", result.get(0).getRase());
        assertEquals("Tysk Modeneser (Modeneser)", result.get(1).getRase());
        assertEquals("Norsk Tomler (Tomler)", result.get(2).getRase());
        verify(dueRepository).findByPaamelding_Utstilling_Id(testUtstillingId);
    }

    @Test
    void saveAll_shouldSaveAndReturnDues() {
        // Arrange
        List<Due> dueList = Arrays.asList(testDue, new Due());
        when(dueRepository.saveAll(dueList)).thenReturn(dueList);

        // Act
        List<Due> result = dueService.saveAll(dueList);

        // Assert
        assertEquals(dueList, result);
        verify(dueRepository).saveAll(dueList);
    }

    @Test
    void finnDueMedId_withExistingId_shouldReturnDue() {
        // Arrange
        when(dueRepository.findById(1L)).thenReturn(Optional.of(testDue));

        // Act
        Due result = dueService.finnDueMedId(1L);

        // Assert
        assertEquals(testDue, result);
        verify(dueRepository).findById(1L);
    }

    @Test
    void finnDueMedId_withNonExistingId_shouldThrowException() {
        // Arrange
        when(dueRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> dueService.finnDueMedId(999L)
        );
        assertEquals("Due with id 999 not found", exception.getMessage());
        verify(dueRepository).findById(999L);
    }

    @Test
    void oppdaterDueInfo_withExistingId_shouldUpdateAndReturnDue() {
        // Arrange
        when(dueRepository.findById(1L)).thenReturn(Optional.of(testDue));
        when(dueLookupService.finnRaseMedId(2L)).thenReturn(new Rase(2L, "Dansk Tomler", "Tomler"));
        when(dueLookupService.finnFargeMedId(3L)).thenReturn(new Farge(3L, "Blå"));
        when(dueLookupService.finnVariantMedId(4L)).thenReturn(new Variant(4L, "Skjellet"));
        when(dueRepository.save(testDue)).thenReturn(testDue);

        // Act
        Due result = dueService.oppdaterDueInfo(1L, 2L, 3L, 4L);

        // Assert
        assertEquals("Dansk Tomler (Tomler)", result.getRase());
        assertEquals("Blå", result.getFarge());
        assertEquals("Skjellet", result.getVariant());
        verify(dueRepository).findById(1L);
        verify(dueRepository).save(testDue);
    }

    @Test
    void oppdaterDueInfo_withNonExistingId_shouldThrowException() {
        // Arrange
        when(dueRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> dueService.oppdaterDueInfo(999L, 2L, 3L, 4L)
        );
        assertEquals("Due with id 999 not found", exception.getMessage());
        verify(dueRepository).findById(999L);
        verify(dueRepository, never()).save(any());
    }

    @Test
    void endreRasePaDuer_shouldCallRepositoryMethod() {
        Rase rase = new Rase(2L, "Dansk Tomler", "Tomler");
        when(dueLookupService.finnRaseMedId(2L)).thenReturn(rase);

        // Act
        dueService.endreRasePaDuer(2L, testDueIdList);

        // Assert
        verify(dueRepository).updateRaseForIds(rase, testDueIdList);
    }

    @Test
    void endreFargePaDuer_shouldCallRepositoryMethod() {
        Farge farge = new Farge(3L, "Blå");
        when(dueLookupService.finnFargeMedId(3L)).thenReturn(farge);

        // Act
        dueService.endreFargePaDuer(3L, testDueIdList);

        // Assert
        verify(dueRepository).updateFargeForIds(farge, testDueIdList);
    }

    @Test
    void endreVariantPaDuer_shouldCallRepositoryMethod() {
        Variant variant = new Variant(4L, "Skjellet");
        when(dueLookupService.finnVariantMedId(4L)).thenReturn(variant);

        // Act
        dueService.endreVariantPaDuer(4L, testDueIdList);

        // Assert
        verify(dueRepository).updateVariantForIds(variant, testDueIdList);
    }
}
