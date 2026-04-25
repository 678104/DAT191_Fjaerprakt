package no.hvl.peristeri.config;

import no.hvl.peristeri.feature.kontaktperson.Kontaktperson;
import no.hvl.peristeri.feature.kontaktperson.KontaktpersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultKontaktpersonCreatorTest {

    @Mock
    private KontaktpersonRepository kontaktpersonRepository;

    @InjectMocks
    private DefaultKontaktpersonCreator defaultKontaktpersonCreator;

    @Test
    void run_shouldCreateDefaultKontaktperson_whenMissing() {
        when(kontaktpersonRepository.existsByRolleIgnoreCaseAndNavnIgnoreCase("Formann", "Johny Larsen"))
                .thenReturn(false);

        defaultKontaktpersonCreator.run();

        ArgumentCaptor<Kontaktperson> captor = ArgumentCaptor.forClass(Kontaktperson.class);
        verify(kontaktpersonRepository).save(captor.capture());

        Kontaktperson lagret = captor.getValue();
        assertEquals("Formann", lagret.getRolle());
        assertEquals("Johny Larsen", lagret.getNavn());
        assertEquals("92150111", lagret.getTelefon());
        assertNull(lagret.getEpost());
    }

    @Test
    void run_shouldNotCreateDefaultKontaktperson_whenAlreadyExists() {
        when(kontaktpersonRepository.existsByRolleIgnoreCaseAndNavnIgnoreCase("Formann", "Johny Larsen"))
                .thenReturn(true);

        defaultKontaktpersonCreator.run();

        verify(kontaktpersonRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

