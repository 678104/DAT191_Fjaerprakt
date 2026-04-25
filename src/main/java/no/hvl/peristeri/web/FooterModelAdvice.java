package no.hvl.peristeri.web;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.kontaktperson.Kontaktperson;
import no.hvl.peristeri.feature.kontaktperson.KontaktpersonService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class FooterModelAdvice {

    private final KontaktpersonService kontaktpersonService;

    @ModelAttribute("footerKontaktpersoner")
    public List<Kontaktperson> footerKontaktpersoner() {
        return kontaktpersonService.hentAlle();
    }
}

