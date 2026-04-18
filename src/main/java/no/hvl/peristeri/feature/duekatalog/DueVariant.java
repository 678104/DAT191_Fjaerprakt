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
@Table(name = "due_variant", uniqueConstraints = @UniqueConstraint(columnNames = "navn"))
public class DueVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String navn;

    public DueVariant(String navn) {
        this.navn = navn;
    }
}

