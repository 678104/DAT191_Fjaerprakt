package no.hvl.peristeri.feature.kontaktperson;

import no.hvl.peristeri.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KontaktpersonServiceImplTest {

    @Mock
    private KontaktpersonRepository kontaktpersonRepository;

    @InjectMocks
    private KontaktpersonServiceImpl kontaktpersonService;

    @Test
    void opprett_shouldTrimValues_andSave() {
        Kontaktperson input = new Kontaktperson();
        input.setRolle(" Formann ");
        input.setNavn(" Johny Larsen ");
        input.setTelefon(" 12 34 56 78 ");
        input.setEpost(" test@mail.no ");

        when(kontaktpersonRepository.save(any(Kontaktperson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Kontaktperson lagret = kontaktpersonService.opprett(input);

        assertEquals("Formann", lagret.getRolle());
        assertEquals("Johny Larsen", lagret.getNavn());
        assertEquals("12 34 56 78", lagret.getTelefon());
        assertEquals("test@mail.no", lagret.getEpost());
        verify(kontaktpersonRepository).save(any(Kontaktperson.class));
    }

    @Test
    void oppdater_shouldSetOptionalFieldsToNull_whenBlank() {
        Kontaktperson eksisterende = new Kontaktperson();
        eksisterende.setId(1L);
        eksisterende.setRolle("Nestleder");
        eksisterende.setNavn("Kari Nordmann");
        eksisterende.setTelefon("123");
        eksisterende.setEpost("kari@mail.no");

        Kontaktperson input = new Kontaktperson();
        input.setRolle(" Styremedlem ");
        input.setNavn(" Ola Nordmann ");
        input.setTelefon("   ");
        input.setEpost("");

        when(kontaktpersonRepository.findById(1L)).thenReturn(Optional.of(eksisterende));
        when(kontaktpersonRepository.save(any(Kontaktperson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Kontaktperson oppdatert = kontaktpersonService.oppdater(1L, input);

        assertEquals("Styremedlem", oppdatert.getRolle());
        assertEquals("Ola Nordmann", oppdatert.getNavn());
        assertNull(oppdatert.getTelefon());
        assertNull(oppdatert.getEpost());
    }

    @Test
    void slett_shouldThrow_whenKontaktpersonMissing() {
        when(kontaktpersonRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> kontaktpersonService.slett(99L));

        verify(kontaktpersonRepository, never()).deleteById(any());
    }
}

