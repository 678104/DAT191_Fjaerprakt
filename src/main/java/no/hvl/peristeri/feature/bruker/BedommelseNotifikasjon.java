package no.hvl.peristeri.feature.bruker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.hvl.peristeri.feature.due.Due;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BedommelseNotifikasjon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mottaker_id", nullable = false)
    private Bruker mottaker;

    @ManyToOne(optional = false)
    @JoinColumn(name = "due_id", nullable = false)
    private Due due;

    @Column(nullable = false)
    private LocalDateTime opprettetTidspunkt;

    private LocalDateTime lestTidspunkt;

    public BedommelseNotifikasjon(Bruker mottaker, Due due) {
        this.mottaker = mottaker;
        this.due = due;
        this.opprettetTidspunkt = LocalDateTime.now();
    }
}

