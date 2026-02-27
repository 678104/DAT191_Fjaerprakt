package no.hvl.peristeri.feature.dommer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.due.Due;

import java.time.LocalDateTime;

/**
 * En bedømmelse av en due
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bedommelse {
    /**
     * Id til bedømmelsen
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long      id;
    private Integer poeng;
    private String fordeler;
    private String onsker;
    private String    feil;
    private LocalDateTime bedommelsesTidspunkt;

    /**
     * Due som bedømmelsen gjelder
     */
    @OneToOne
    @JoinColumn(name = "due_id", referencedColumnName = "ID")
    private Due due;

    @ManyToOne
    @JoinColumn(name = "bedomt_av", referencedColumnName = "ID")
    private DommerPaamelding bedomtAv;

    public Bedommelse(Integer poeng, String fordeler, String onsker, String feil) {
        this.poeng = poeng;
        this.fordeler = fordeler;
        this.onsker = onsker;
        this.feil = feil;
        bedommelsesTidspunkt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (bedommelsesTidspunkt == null) {
            bedommelsesTidspunkt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        bedommelsesTidspunkt = LocalDateTime.now();
    }
}
