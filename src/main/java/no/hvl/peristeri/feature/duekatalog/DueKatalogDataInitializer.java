package no.hvl.peristeri.feature.duekatalog;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DueKatalogDataInitializer implements CommandLineRunner {

    private final DueGruppeRepository dueGruppeRepository;
    private final DueRaseRepository dueRaseRepository;
    private final DueFargeRepository dueFargeRepository;
    private final DueVariantRepository dueVariantRepository;

    @Override
    public void run(String... args) {
        if (harEksisterendeData()) {
            return;
        }

        Map<String, List<String>> grupperMedRaser = Map.ofEntries(
                Map.entry("Norske Raser", List.of("Norsk Tomler", "Bergens Tomler", "Norsk Mefikk")),
                Map.entry("Formduer", List.of("Damacener", "Tysk skjønnhetsbrevdue")),
                Map.entry("Vorteduer", List.of("Carrier", "Spansk Jordbærøye")),
                Map.entry("Hønseduer", List.of("Engelsk Modena Schetti", "Engelsk Modena Gazzi", "Tysk Modeneser Schetti", "Tysk Modeneser Gazzi")),
                Map.entry("Kroppert - Pustere", List.of("Vorburger Kroppert", "Brunner Kroppert", "Norwich Puster", "Amsterdamer")),
                Map.entry("Fargeduer", List.of("Nurnberg Svale", "Nurnberg Lerke")),
                Map.entry("Trommeduer", List.of("Tysk dobbeltkappet Trommer")),
                Map.entry("Strukturduer", List.of("Indisk Høystjert", "Engelsk Høystjert")),
                Map.entry("Mefikker", List.of("Gml. Tysk Mefikk", "Italiensk Mefikk", "Afrikansk Mefikk", "Gml. Orientalsk Mefikk", "Aachener Lakkvinge")),
                Map.entry("Utenlandske Tomlere", List.of("Dansk Tomler", "Tysk Show Tippler", "Limerick Tumler", "Tysk Nonne", "Hamburg Skimmel"))
        );

        List<String> farger = List.of("Hvit", "Rød", "Gul", "Blå");
        List<String> varianter = List.of("Ternet", "m/ bånd", "u/ bånd", "Skimlet", "m/ krone");

        grupperMedRaser.forEach((gruppeNavn, raser) -> {
            DueGruppe gruppe = dueGruppeRepository.save(new DueGruppe(gruppeNavn));
            raser.forEach(raseNavn -> dueRaseRepository.save(new DueRase(raseNavn, gruppe)));
        });

        farger.forEach(farge -> dueFargeRepository.save(new DueFarge(farge)));
        varianter.forEach(variant -> dueVariantRepository.save(new DueVariant(variant)));
    }

    private boolean harEksisterendeData() {
        return dueGruppeRepository.count() > 0
                || dueRaseRepository.count() > 0
                || dueFargeRepository.count() > 0
                || dueVariantRepository.count() > 0;
    }
}

