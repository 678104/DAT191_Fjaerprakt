package no.hvl.peristeri.feature.dommer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KategoriKommentar {
    @Column(name = "standard_kommentar")
    private String standardKommentar;

    @Column(name = "fritekst_kommentar", length = 2000)
    private String fritekstKommentar;
}

