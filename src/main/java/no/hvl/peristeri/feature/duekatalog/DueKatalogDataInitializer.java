package no.hvl.peristeri.feature.duekatalog;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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

        Map<String, List<String>> grupperMedRaser = new LinkedHashMap<>();
        grupperMedRaser.put("Norske Raser", List.of("Norsk Tomler", "Bergens Tomler", "Norsk Mefikk"));
        grupperMedRaser.put("Formduer", List.of("Damacener", "Tysk skjønnhetsbrevdue"));
        grupperMedRaser.put("Vorteduer", List.of("Carrier", "Spansk Jordbærøye"));
        grupperMedRaser.put("Hønseduer", List.of("Engelsk Modena Schetti", "Engelsk Modena Gazzi", "Tysk Modeneser Schetti", "Tysk Modeneser Gazzi"));
        grupperMedRaser.put("Kroppert - Pustere", List.of("Vorburger Kroppert", "Brunner Kroppert", "Norwich Puster", "Amsterdamer"));
        grupperMedRaser.put("Fargeduer", List.of("Nurnberg Svale", "Nurnberg Lerke"));
        grupperMedRaser.put("Trommeduer", List.of("Tysk dobbeltkappet Trommer"));
        grupperMedRaser.put("Strukturduer", List.of("Indisk Høystjert", "Engelsk Høystjert"));
        grupperMedRaser.put("Mefikker", List.of("Gml. Tysk Mefikk", "Italiensk Mefikk", "Afrikansk Mefikk", "Gml. Orientalsk Mefikk", "Aachener Lakkvinge"));
        grupperMedRaser.put("Utenlandske Tomlere", List.of("Dansk Tomler", "Tysk Show Tippler", "Limerick Tumler", "Tysk Nonne", "Hamburg Skimmel"));

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

