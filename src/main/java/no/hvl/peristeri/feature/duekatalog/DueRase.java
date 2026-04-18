package no.hvl.peristeri.feature.duekatalog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "due_rase",
        uniqueConstraints = @UniqueConstraint(columnNames = {"gruppe_id", "navn"})
)
public class DueRase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String navn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gruppe_id", nullable = false)
    private DueGruppe gruppe;

    public DueRase(String navn, DueGruppe gruppe) {
        this.navn = navn;
        this.gruppe = gruppe;
    }
}

