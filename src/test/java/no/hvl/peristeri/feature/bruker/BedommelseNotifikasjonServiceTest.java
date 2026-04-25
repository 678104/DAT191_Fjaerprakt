package no.hvl.peristeri.feature.bruker;

import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BedommelseNotifikasjonServiceTest {

    @Mock
    private BedommelseNotifikasjonRepository bedommelseNotifikasjonRepository;

    @InjectMocks
    private BedommelseNotifikasjonService bedommelseNotifikasjonService;

    private Bruker eier;
    private Due due;

    @BeforeEach
    void setUp() {
        eier = new Bruker();
        eier.setId(1L);

        Paamelding paamelding = new Paamelding();
        paamelding.setUtstiller(eier);

        due = new Due();
        due.setId(10L);
        due.setPaamelding(paamelding);
    }

    @Test
    void opprettVedFerdigBedommelse_skalLagreNyNotifikasjonNarIngenUlestFinnes() {
        when(bedommelseNotifikasjonRepository.existsByMottaker_IdAndDue_IdAndLestTidspunktIsNull(1L, 10L))
                .thenReturn(false);

        bedommelseNotifikasjonService.opprettVedFerdigBedommelse(due);

        verify(bedommelseNotifikasjonRepository).save(org.mockito.ArgumentMatchers.any(BedommelseNotifikasjon.class));
    }

    @Test
    void opprettVedFerdigBedommelse_skalIkkeLagreNarUlestAlleredeFinnes() {
        when(bedommelseNotifikasjonRepository.existsByMottaker_IdAndDue_IdAndLestTidspunktIsNull(1L, 10L))
                .thenReturn(true);

        bedommelseNotifikasjonService.opprettVedFerdigBedommelse(due);

        verify(bedommelseNotifikasjonRepository, never()).save(org.mockito.ArgumentMatchers.any(BedommelseNotifikasjon.class));
    }
}

