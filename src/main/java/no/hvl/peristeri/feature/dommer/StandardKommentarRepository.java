package no.hvl.peristeri.feature.dommer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandardKommentarRepository extends JpaRepository<StandardKommentar, Long> {
	List<StandardKommentar> findByTypeOrderByKategoriAscIdAsc(StandardKommentarType type);

	boolean existsByTypeAndKategoriAndTekstIgnoreCase(StandardKommentarType type, BedommingsKategori kategori, String tekst);
}

