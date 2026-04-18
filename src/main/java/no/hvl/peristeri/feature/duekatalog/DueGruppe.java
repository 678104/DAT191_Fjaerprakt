package no.hvl.peristeri.feature.duekatalog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "due_gruppe", uniqueConstraints = @UniqueConstraint(columnNames = "navn"))
public class DueGruppe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String navn;

    @OneToMany(mappedBy = "gruppe", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DueRase> raser = new LinkedHashSet<>();

    public DueGruppe(String navn) {
        this.navn = navn;
    }
}

