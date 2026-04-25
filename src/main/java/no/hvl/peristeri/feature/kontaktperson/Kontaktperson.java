package no.hvl.peristeri.feature.kontaktperson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Kontaktperson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Rolle er obligatorisk")
    @Size(max = 100, message = "Rolle kan ikke vaere lengre enn 100 tegn")
    @Column(nullable = false, length = 100)
    private String rolle;

    @NotBlank(message = "Navn er obligatorisk")
    @Size(max = 150, message = "Navn kan ikke vaere lengre enn 150 tegn")
    @Column(nullable = false, length = 150)
    private String navn;

    @Size(max = 50, message = "Telefonnummer kan ikke vaere lengre enn 50 tegn")
    @Column(length = 50)
    private String telefon;

    @Email(message = "E-post er ugyldig")
    @Size(max = 200, message = "E-post kan ikke vaere lengre enn 200 tegn")
    @Column(length = 200)
    private String epost;

    public Kontaktperson(String rolle, String navn, String telefon, String epost) {
        this.rolle = rolle;
        this.navn = navn;
        this.telefon = telefon;
        this.epost = epost;
    }
}

