package no.hvl.peristeri.feature.bruker;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bruker implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fornavn;
    @Column(nullable = false)
    private String etternavn;
    private String adresse;
    private String postnummer;
    private String poststed;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String epost;
    private String telefon;
    private String forening;

    @Column(nullable = false)
    private String password;

    private Boolean aktivert = true;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Rolle> roller = new HashSet<>();

    @OneToMany(mappedBy = "utstiller", cascade = CascadeType.ALL)
    private List<Paamelding> paamelding;

    /**
     * Konstruktør for å opprette en bruker med spesifisert rolle
     *
     * @param fornavn   Brukerens fornavn
     * @param etternavn Brukerens etternavn
     * @param adresse   Brukerens adresse
     * @param epost     Brukerens epost
     * @param telefon   Brukerens telefonnummer
     * @param forening  Brukerens forening
     * @param rolle     Brukerens rolle
     */
    public Bruker(String fornavn, String etternavn, String adresse, String epost, String telefon, String forening, Rolle rolle) {
        this.fornavn = fornavn;
        this.etternavn = etternavn;
        this.adresse = adresse;
        this.epost = epost;
        this.telefon = telefon;
        this.forening = forening;
        leggTilRolle(rolle);
    }

    /**
     * Konstruktør for å opprette en bruker med standard rolle UTSTILLER
     *
     * @param fornavn   Brukerens fornavn
     * @param etternavn Brukerens etternavn
     * @param adresse   Brukerens adresse
     * @param epost     Brukerens epost
     * @param telefon   Brukerens telefonnummer
     * @param forening  Brukerens forening
     */
    public Bruker(String fornavn, String etternavn, String adresse, String epost, String telefon, String forening) {
        this.fornavn = fornavn;
        this.etternavn = etternavn;
        this.adresse = adresse;
        this.epost = epost;
        this.telefon = telefon;
        this.forening = forening;
        leggTilRolle(Rolle.UTSTILLER);
    }

    /**
     * Henter brukerens fulle navn
     *
     * @return Brukerens fulle navn i formatet "Fornavn Etternavn"
     */
    public String getNavn() {
        return fornavn + " " + etternavn;
    }

    public void leggTilRolle(Rolle rolle) {
        if (roller == null) {
            roller = new HashSet<>();
        }
        roller.add(rolle);
    }

    public void fjernRolle(Rolle rolle) {
        if (roller == null) {
            roller = new HashSet<>();
        }
        roller.remove(rolle);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoller().stream()
                                .map(role -> new SimpleGrantedAuthority(
                                        "ROLE_" + role.name())) // Spring expects "ROLE_****"
                                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return epost;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "Bruker{" +
                "id=" + id +
                ", fornavn='" + fornavn + '\'' +
                ", etternavn='" + etternavn + '\'' +
                ", epost='" + epost + '\'';
    }

}
