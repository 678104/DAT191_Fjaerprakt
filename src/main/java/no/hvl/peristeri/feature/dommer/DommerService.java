package no.hvl.peristeri.feature.dommer;

import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;

import java.util.List;

public interface DommerService {
	List<Due> hentAlleDuer();

	void lagreBedommelse(Long dueId, Bedommelse nyBedommelse, Bruker dommer);

	List<Due> finnDuerEtterBurnummer(Integer burnummer);

	List<DommerPaamelding> finnDommerPaameldingerTilUtstilling(Long utstillingId);

	DommerPaamelding lagreDommerPaamelding(Bruker dommer, Long utstillingId, String passord);

	DommerPaamelding lagreDommerPaaMelding(DommerPaamelding dommerPaamelding);

	void fjernDommerPaamelding(Long dommerPaameldingId);

	DommerPaamelding fordelRaserTilDommer(Long dommerPaameldingId, List<String> raser);

	Due hentDueMedId(Long dueId);

	List<Due> finnDuerDommerSkalBedomme(Bruker dommer);

	Due finnDueDommerSkalBedommeMedBurnummer(Bruker dommer, Integer burnummer);
}
