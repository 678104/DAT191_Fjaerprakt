package no.hvl.peristeri.feature.dommer;

import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.due.Due;

import java.util.List;

public interface DommerService {
	List<Due> hentAlleDuer();

	void lagreBedommelse(Long dueId, Bedommelse nyBedommelse, Bruker dommer);

	void lagreBedommelse(Long dueId, Bedommelse nyBedommelse, Bruker dommer, Long utstillingId);

	List<Due> finnDuerEtterBurnummer(Integer burnummer);

	List<DommerPaamelding> finnDommerPaameldingerTilUtstilling(Long utstillingId);

	DommerPaamelding lagreDommerPaamelding(Bruker dommer, Long utstillingId, String passord);

	DommerPaamelding lagreDommerPaaMelding(DommerPaamelding dommerPaamelding);

	void fjernDommerPaamelding(Long dommerPaameldingId);

	DommerPaamelding fordelRaserTilDommer(Long dommerPaameldingId, List<String> raser);

	Due tilordneDueTilDommer(Long dommerPaameldingId, Long dueId);

	List<Due> tilordneDuerTilDommer(Long dommerPaameldingId, List<Long> dueIder);

	List<Due> tilordneDuerTilDommerEtterRaser(Long dommerPaameldingId, Long utstillingId, List<String> raser);

	Due hentDueMedId(Long dueId);

	List<DommerPaamelding> finnDommerPaameldinger(Bruker dommer);

	List<Due> finnDuerDommerSkalBedomme(Bruker dommer);

	List<Due> finnDuerDommerSkalBedomme(Bruker dommer, Long utstillingId);

	Due finnDueDommerSkalBedommeMedBurnummer(Bruker dommer, Integer burnummer);

	Due finnDueDommerSkalBedommeMedBurnummer(Bruker dommer, Integer burnummer, Long utstillingId);
}
