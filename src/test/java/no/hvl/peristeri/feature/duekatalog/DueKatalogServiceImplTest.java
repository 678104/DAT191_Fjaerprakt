package no.hvl.peristeri.feature.duekatalog;

import no.hvl.peristeri.common.exception.BusinessRuleViolationException;
import no.hvl.peristeri.common.exception.InvalidParameterException;
import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueKatalogServiceImplTest {

    @Mock
    private DueGruppeRepository dueGruppeRepository;
    @Mock
    private DueRaseRepository dueRaseRepository;
    @Mock
    private DueFargeRepository dueFargeRepository;
    @Mock
    private DueVariantRepository dueVariantRepository;

    @InjectMocks
    private DueKatalogServiceImpl dueKatalogService;

    private DueGruppe testGruppe;

    @BeforeEach
    void setUp() {
        testGruppe = new DueGruppe();
        testGruppe.setId(1L);
        testGruppe.setNavn("Norske Raser");
    }

    @Test
    void opprettGruppe_shouldSaveGroup_whenNameIsUnique() {
        when(dueGruppeRepository.existsByNavnIgnoreCase("Norske Raser")).thenReturn(false);
        when(dueGruppeRepository.save(any(DueGruppe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DueGruppe result = dueKatalogService.opprettGruppe(" Norske Raser ");

        assertEquals("Norske Raser", result.getNavn());
        verify(dueGruppeRepository).save(any(DueGruppe.class));
    }

    @Test
    void opprettRase_shouldThrowBusinessRuleViolation_whenRaceAlreadyExistsInGroup() {
        when(dueRaseRepository.existsByGruppeIdAndNavnIgnoreCase(1L, "Norsk Tomler")).thenReturn(true);

        assertThrows(BusinessRuleViolationException.class,
                () -> dueKatalogService.opprettRase(1L, "Norsk Tomler"));

        verify(dueRaseRepository, never()).save(any());
    }

    @Test
    void opprettRase_shouldThrowResourceNotFound_whenGroupMissing() {
        when(dueRaseRepository.existsByGruppeIdAndNavnIgnoreCase(1L, "Norsk Tomler")).thenReturn(false);
        when(dueGruppeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dueKatalogService.opprettRase(1L, "Norsk Tomler"));
    }

    @Test
    void slettGruppe_shouldThrowBusinessRuleViolation_whenGroupHasRaces() {
        when(dueGruppeRepository.existsById(1L)).thenReturn(true);
        when(dueRaseRepository.countByGruppeId(1L)).thenReturn(2L);

        assertThrows(BusinessRuleViolationException.class,
                () -> dueKatalogService.slettGruppe(1L));

        verify(dueGruppeRepository, never()).deleteById(anyLong());
    }

    @Test
    void slettGruppe_shouldDelete_whenGroupIsEmpty() {
        when(dueGruppeRepository.existsById(1L)).thenReturn(true);
        when(dueRaseRepository.countByGruppeId(1L)).thenReturn(0L);

        dueKatalogService.slettGruppe(1L);

        verify(dueGruppeRepository).deleteById(1L);
    }

    @Test
    void erRaseGyldigForGruppe_shouldReturnFalse_whenMissingInput() {
        assertFalse(dueKatalogService.erRaseGyldigForGruppe(null, "Norsk Tomler"));
        assertFalse(dueKatalogService.erRaseGyldigForGruppe(1L, " "));
    }

    @Test
    void opprettFarge_shouldThrowInvalidParameter_whenBlank() {
        assertThrows(InvalidParameterException.class, () -> dueKatalogService.opprettFarge(" "));
    }
}

