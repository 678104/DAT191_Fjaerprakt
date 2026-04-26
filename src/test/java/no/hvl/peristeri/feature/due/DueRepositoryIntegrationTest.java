package no.hvl.peristeri.feature.due;

import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerRepository;
import no.hvl.peristeri.feature.duekatalog.DueGruppe;
import no.hvl.peristeri.feature.duekatalog.DueGruppeRepository;
import no.hvl.peristeri.feature.duekatalog.DueRase;
import no.hvl.peristeri.feature.duekatalog.DueRaseRepository;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.feature.paamelding.PaameldingRepository;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class DueRepositoryIntegrationTest {

    @Autowired
    private DueRepository dueRepository;

    @Autowired
    private DueRaseRepository dueRaseRepository;

    @Autowired
    private DueGruppeRepository dueGruppeRepository;

    @Autowired
    private BrukerRepository brukerRepository;

    @Autowired
    private PaameldingRepository paameldingRepository;

    @Autowired
    private UtstillingRepository utstillingRepository;

    private Long utstillingId;
    private Paamelding paamelding;

    @BeforeEach
    void setUp() {
        Utstilling utstilling = new Utstilling(
                "Arrangoer",
                "Adresse 1",
                "Vaarutstilling",
                "Test",
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(32),
                LocalDate.now().plusDays(20),
                100.0
        );
        utstilling = utstillingRepository.save(utstilling);
        utstillingId = utstilling.getId();

        Bruker bruker = new Bruker(
                "Test",
                "Utstiller",
                "Adresse 2",
                "test.utstiller@example.com",
                "12345678",
                "Forening"
        );
        bruker.setPassword("passord");
        bruker = brukerRepository.save(bruker);

        paamelding = paameldingRepository.save(new Paamelding(bruker, utstilling));
    }

    @Test
    void hentRaserPaameldtUtstilling_sortererEtterLavesteGruppeId_ogUtenGruppeTilSlutt() {
        DueGruppe gruppe1 = dueGruppeRepository.save(new DueGruppe("Gruppe 1"));
        DueGruppe gruppe2 = dueGruppeRepository.save(new DueGruppe("Gruppe 2"));

        dueRaseRepository.save(new DueRase("Rase B", gruppe2));
        dueRaseRepository.save(new DueRase("Rase A", gruppe1));
        dueRaseRepository.save(new DueRase("Rase Multi", gruppe2));
        dueRaseRepository.save(new DueRase("Rase Multi", gruppe1));

        dueRepository.saveAll(List.of(
                lagDue("Uten Gruppe", 1),
                lagDue("Rase B", 2),
                lagDue("Rase A", 3),
                lagDue("Rase Multi", 4),
                lagDue("Rase A", 5)
        ));

        List<String> resultat = dueRepository.hentRaserPaameldtUtstilling(utstillingId);

        assertEquals(List.of("Rase A", "Rase Multi", "Rase B", "Uten Gruppe"), resultat);
    }

    @Test
    void hentRaserPaameldtUtstilling_brukerEksaktRaseMatch() {
        DueGruppe gruppe = dueGruppeRepository.save(new DueGruppe("Gruppe"));
        dueRaseRepository.save(new DueRase("Eksakt Rase", gruppe));

        dueRepository.saveAll(List.of(
                lagDue("eksakt rase", 10),
                lagDue("Eksakt Rase", 11)
        ));

        List<String> resultat = dueRepository.hentRaserPaameldtUtstilling(utstillingId);

        assertEquals(List.of("Eksakt Rase", "eksakt rase"), resultat);
    }

    private Due lagDue(String rase, int burnummer) {
        Due due = new Due();
        due.setRase(rase);
        due.setBurnummer(burnummer);
        due.setPaamelding(paamelding);
        return due;
    }
}

