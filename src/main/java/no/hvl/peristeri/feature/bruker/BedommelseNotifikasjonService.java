package no.hvl.peristeri.feature.bruker;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.due.Due;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BedommelseNotifikasjonService {

    private final BedommelseNotifikasjonRepository bedommelseNotifikasjonRepository;

    @Transactional
    public void opprettVedFerdigBedommelse(Due due) {
        if (due == null || due.getId() == null || due.getPaamelding() == null || due.getPaamelding().getUtstiller() == null) {
            return;
        }

        Bruker mottaker = due.getPaamelding().getUtstiller();
        boolean finnesUlestNotifikasjon = bedommelseNotifikasjonRepository
                .existsByMottaker_IdAndDue_IdAndLestTidspunktIsNull(mottaker.getId(), due.getId());

        if (!finnesUlestNotifikasjon) {
            bedommelseNotifikasjonRepository.save(new BedommelseNotifikasjon(mottaker, due));
        }
    }

    @Transactional(readOnly = true)
    public long tellUleste(Long mottakerId) {
        if (mottakerId == null) {
            return 0;
        }
        return bedommelseNotifikasjonRepository.countByMottaker_IdAndLestTidspunktIsNull(mottakerId);
    }

    @Transactional
    public void markerSomLestForPaamelding(Long mottakerId, Long paameldingId) {
        if (mottakerId == null || paameldingId == null) {
            return;
        }
        bedommelseNotifikasjonRepository.markerSomLestForPaamelding(mottakerId, paameldingId, LocalDateTime.now());
    }
}

