package no.hvl.peristeri.feature.dommer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.due.Due;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

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
    @Column(length = 1000)
    private String fordeler;
    private String onsker;
    private String    feil;
    private LocalDateTime bedommelsesTidspunkt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bedommelse_kategorier", joinColumns = @JoinColumn(name = "bedommelse_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "kategori")
    private Map<BedommingsKategori, KategoriKommentar> kategorier = new EnumMap<>(BedommingsKategori.class);

    /**
     * Due som bedømmelsen gjelder
     */
    @OneToOne
    @JoinColumn(name = "due_id", referencedColumnName = "ID")
    private Due due;

    @ManyToOne
    @JoinColumn(name = "bedomt_av", referencedColumnName = "ID")
    private DommerPaamelding bedomtAv;

    @OneToOne(mappedBy = "bedommelse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BedommelseBilde bilde;

    @Transient
    private boolean fjernBilde;

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

    public void setBilde(BedommelseBilde bilde) {
        this.bilde = bilde;
        if (bilde != null) {
            bilde.setBedommelse(this);
        }
    }
}
