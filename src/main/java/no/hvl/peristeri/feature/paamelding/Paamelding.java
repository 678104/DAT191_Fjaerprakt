package no.hvl.peristeri.feature.paamelding;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.utstilling.Utstilling;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Paamelding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paameldings_avgift")
    private BigDecimal paameldingsAvgift;

    @ManyToOne
    @JoinColumn(name = "utstiller_id", nullable = false)
    private Bruker utstiller;

    @OneToMany(mappedBy = "paamelding", cascade = CascadeType.ALL)
    private List<Due> duer;

    @ManyToOne
    @JoinColumn(name = "utstilling_id", nullable = false)
    private Utstilling utstilling;

    public Paamelding(Bruker utstiller, Utstilling utstilling) {
        this.utstiller = utstiller;
        this.utstilling = utstilling;
    }
}
