package no.hvl.peristeri.config;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.kontaktperson.Kontaktperson;
import no.hvl.peristeri.feature.kontaktperson.KontaktpersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DefaultKontaktpersonCreator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DefaultKontaktpersonCreator.class);

    private static final String STANDARD_ROLLE = "Formann";
    private static final String STANDARD_NAVN = "Johny Larsen";
    private static final String STANDARD_TELEFON = "92150111";

    private final KontaktpersonRepository kontaktpersonRepository;

    @Override
    public void run(String... args) {
        boolean finnes = kontaktpersonRepository.existsByRolleIgnoreCaseAndNavnIgnoreCase(
                STANDARD_ROLLE,
                STANDARD_NAVN
        );

        if (finnes) {
            logger.debug("Standard kontaktperson finnes allerede. Hopper over opprettelse.");
            return;
        }

        Kontaktperson kontaktperson = new Kontaktperson(
                STANDARD_ROLLE,
                STANDARD_NAVN,
                STANDARD_TELEFON,
                null
        );
        kontaktpersonRepository.save(kontaktperson);
        logger.info("La til standard kontaktperson: {} {}", STANDARD_ROLLE, STANDARD_NAVN);
    }
}

