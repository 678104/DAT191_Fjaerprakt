package no.hvl.peristeri.common;

import lombok.RequiredArgsConstructor;
import no.hvl.peristeri.feature.bruker.Bruker;
import no.hvl.peristeri.feature.bruker.BrukerService;
import no.hvl.peristeri.feature.bruker.Rolle;
import no.hvl.peristeri.feature.dommer.Bedommelse;
import no.hvl.peristeri.feature.dommer.DommerPaamelding;
import no.hvl.peristeri.feature.dommer.DommerService;
import no.hvl.peristeri.feature.due.Due;
import no.hvl.peristeri.feature.due.DueService;
import no.hvl.peristeri.feature.paamelding.Paamelding;
import no.hvl.peristeri.feature.paamelding.PaameldingService;
import no.hvl.peristeri.feature.utstilling.Utstilling;
import no.hvl.peristeri.feature.utstilling.UtstillingService;
import no.hvl.peristeri.util.RaseStringHjelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Denne klassen brukes for å opprette testdata i databasen.<br>
 * Klassen blir kun brukt hvis profilen "dev" eller "demo" er satt.
 */
@RequiredArgsConstructor
@Component
@Profile({"dev", "demo"})
public class SampleDataCreator implements CommandLineRunner {

	private final Logger logger = LoggerFactory.getLogger(SampleDataCreator.class);

	private final UtstillingService utstillingService;
	private final BrukerService     brukerService;
	private final DueService        dueService;
	private final DommerService     dommerService;
	private final PaameldingService paameldingService;
	private final PasswordEncoder   passwordEncoder;

	@Override
	public void run(String... args) {
		if (brukerService.findByEpost("admin@peristeri.no").isPresent()) {
			logger.info("Sample data already exists. Skipping data creation.");
			return;
		}

		lagAdminBruker();

		utstillingService.addAll(utstillingerTestData());

		lagEnTestUtstilling();
		lagDommerTestData();
		utstillingMedMangeDuer();
	}

	private void setPassword(Bruker bruker, String nyttPassord) {
		bruker.setPassword(passwordEncoder.encode(nyttPassord));
	}

	private List<Utstilling> utstillingerTestData() {
		var list = new ArrayList<Utstilling>();
		Utstilling a = new Utstilling("Bergen Praktdueforening", "Bergen", "Vårutstilling med BPDF",
				"Her er en kort beskrivelse som sier noe om hva som skjer på utstillingen", LocalDate.now().plusDays(3),
				LocalDate.now().plusDays(5), LocalDate.now().minusDays(1), 100.0);
		list.add(a);
		Utstilling b = new Utstilling("Bergen Praktdueforening", "Bergen", "Vinterutstilling med BPDF",
				"Dette er en test utstilling lagt til for å vise en utstilling som er avsluttet",
				LocalDate.now().minusMonths(2).minusDays(5), LocalDate.now().minusMonths(2),
				LocalDate.now().minusMonths(3).minusDays(1), 100.0);
		list.add(b);
		Utstilling c = new Utstilling("Norges Brevdueforbund", "Dueland", "Test åpen paamelding",
				"Dette er en test utstilling lagt til for å teste å vise et arrangement med åpen påmelding",
				LocalDate.now().plusMonths(5), LocalDate.now().plusMonths(5).plusDays(2), LocalDate.now().plusMonths(4),
				100.0); // Åpen for påmelding
		c.setPaameldingAApnet(true);
		list.add(c);
		Utstilling d = new Utstilling("Brann", "Brann Stadion", "Test åpen paamelding",
				"Dette er en test utstilling lagt til for å teste funksjonalitet, spesielt det å vise en aktiv utstilling",
				LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(2).plusDays(2), LocalDate.now().plusMonths(1),
				100.0); // Åpen for påmelding
		d.setPaameldingAApnet(true);
		list.add(d);
		return list;
	}

	private void lagAdminBruker() {
		Bruker admin = new Bruker("admin", "test", "admingaten 2", "admin@peristeri.no", "999999999", "Peristeri",
				Rolle.ADMIN);
		setPassword(admin, "admin");
		admin = brukerService.lagreBruker(admin);
	}

	private void lagEnTestUtstilling() {

		Bruker storm = new Bruker("Storm", "Sangolt", "Nordeidevegen 69", "sangolts@gmail.com", "87654321",
				"Norsk Forening for Tamduer");
		setPassword(storm, "storm");
		storm = brukerService.lagreBruker(storm);

		Bruker cris = new Bruker("Cristopher", "Rojas", "Baune", "cri.roj@gmail.com", "12345678",
				"Forening for Chileanske duer");
		setPassword(cris, "cris");
		cris = brukerService.lagreBruker(cris);

		Bruker bell = new Bruker("Kristian", "Bell", "Faurde", "bellemann@outlook.com", "13579753",
				"Forening for Amerikanske duer");
		setPassword(bell, "bell");
		bell = brukerService.lagreBruker(bell);


		Utstilling u = utstillingService.leggTilUtstilling(
				new Utstilling("Peristeri", "Bergen", "Test aktiv utstilling",
						"Dette er en test utstilling lagt til for å vise en aktiv utstilling",
						LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), LocalDate.now().minusDays(28),
						100.0));
		u = utstillingService.setAktivUtstilling(u.getId());

		Bruker dommer = new Bruker("Dommer for", "Test Aktiv Utstilling", "Et sted", "dommer@peristeri.no", "123456789",
				"Dommerforeningen", Rolle.DOMMER);
		DommerPaamelding dp = dommerService.lagreDommerPaamelding(dommer, u.getId(), "dommer");

		Paamelding p1 = new Paamelding(storm, u);
		Paamelding p2 = new Paamelding(cris, u);
		Paamelding p3 = new Paamelding(bell, u);

		Due ds1 = new Due("1-23", "Norsk Tomler", "Hvit", "Standard", true, false, false, p1);
		ds1.setPaamelding(p1);
		Due ds2 = new Due("2-23", "Norsk Tomler", "Grå", "Standard", true, false, false, p1);
		ds2.setPaamelding(p1);
		Due ds3 = new Due("3-23", "Norsk Tomler", "Blå", "Sjelden", false, true, false, p1);
		ds3.setPaamelding(p1);
		Due ds4 = new Due("4-23", "Norsk Tomler", "Oransje", "Sjelden", false, true, false, p1);
		ds4.setPaamelding(p1);
		Due dc1 = new Due("5-23", "Engelsk Tippler", "Rød", "Lang", false, true, false, p2);
		dc1.setPaamelding(p2);
		Due db1 = new Due("6-23", "Bergensprakt", "Blå", "Kort", false, false, true, p3);
		db1.setPaamelding(p3);

		Bedommelse b2 = new Bedommelse(94, "Ønsker mer rød", "Fint posture", "Ingen feil");
		b2.setDue(dc1);
		dc1.setBedommelse(b2);


		paameldingService.manueltLeggTilPaamelding(p1);
		paameldingService.manueltLeggTilPaamelding(p2);
		paameldingService.manueltLeggTilPaamelding(p3);


		dueService.leggTilDue(ds1);
		dueService.leggTilDue(ds2);
		dueService.leggTilDue(ds3);
		dueService.leggTilDue(ds4);
		dueService.leggTilDue(dc1);
		dueService.leggTilDue(db1);

		List<String> raser = dueService.hentRaserPaameldtUtstilling(u.getId()).reversed();
		utstillingService.oppdaterSorterteRaser(u.getId(), RaseStringHjelper.konverterTilString(raser));

		dommerService.fordelRaserTilDommer(dp.getId(), raser);

		dommerService.lagreBedommelse(dc1.getId(), b2, dp.getDommer());

		utstillingService.genererBurnumre(u);

	}

	private void lagDommerTestData() {
		Utstilling utstilling = utstillingService.leggTilUtstilling(
				new Utstilling("Dommer Utstilling", "Oslo", "Test utstilling for dommere",
						"Dette er en test utstilling for å teste dommerpåmeldinger", LocalDate.now().plusDays(10),
						LocalDate.now().plusDays(12), LocalDate.now().plusDays(5), 150.0));

		Bruker dommer1 = new Bruker("Dommer1", "Test", "Dommerveien 1", "dommer1@peristeri.no", "123456789",
				"Dommerforeningen",
				Rolle.DOMMER);

		Bruker dommer2 = new Bruker("Dommer2", "Test", "Dommerveien 2", "dommer2@peristeri.no", "987654321",
				"Dommerforeningen",
				Rolle.DOMMER);

		DommerPaamelding dommerPaamelding1 = dommerService.lagreDommerPaamelding(dommer1, utstilling.getId(), "dommer1");
		DommerPaamelding dommerPaamelding2 = dommerService.lagreDommerPaamelding(dommer2, utstilling.getId(), "dommer2");


		Bruker bruker1 = new Bruker("Bruker1", "Eksempel", "Brukerveien 1", "bruker1@test.no", "111111111",
				"Forening A");
		setPassword(bruker1, "bruker1");
		bruker1 = brukerService.lagreBruker(bruker1);
		Bruker bruker2 = new Bruker("Bruker2", "Eksempel", "Brukerveien 2", "bruker2@test.no", "222222222",
				"Forening B");
		setPassword(bruker2, "bruker2");
		bruker2 = brukerService.lagreBruker(bruker2);
		Bruker bruker3 = new Bruker("Bruker3", "Eksempel", "Brukerveien 3", "bruker3@test.no", "333333333",
				"Forening C");
		setPassword(bruker3, "bruker3");
		bruker3 = brukerService.lagreBruker(bruker3);
		Bruker bruker4 = new Bruker("Bruker4", "Eksempel", "Brukerveien 4", "bruker4@test.no", "444444444",
				"Forening D");
		setPassword(bruker4, "bruker4");
		bruker4 = brukerService.lagreBruker(bruker4);

		Paamelding paamelding1 = new Paamelding(bruker1, utstilling);
		Paamelding paamelding2 = new Paamelding(bruker2, utstilling);
		Paamelding paamelding3 = new Paamelding(bruker3, utstilling);
		Paamelding paamelding4 = new Paamelding(bruker4, utstilling);

		Due due1 = new Due("7-23", "Norsk Tomler", "Hvit", "Standard", true, false, false, paamelding1);
		due1.setPaamelding(paamelding1);
		Due due2 = new Due("8-23", "Norsk Tomler", "Svart", "Standard", true, false, false, paamelding1);
		due2.setPaamelding(paamelding1);
		Due due3 = new Due("9-23", "Engelsk Tippler", "Blå", "Lang", false, true, false, paamelding2);
		due3.setPaamelding(paamelding2);
		Due due4 = new Due("10-23", "Engelsk Tippler", "Grønn", "Kort", false, false, true, paamelding2);

		Due due5 = new Due("11-23", "Bergensprakt", "Hvit", "Standard", true, false, false, paamelding3);
		due5.setPaamelding(paamelding3);
		Due due6 = new Due("12-23", "Bergensprakt", "Svart", "Standard", true, false, false, paamelding3);
		due6.setPaamelding(paamelding3);
		Due due7 = new Due("13-23", "Norsk Tomler", "Blå", "Lang", false, true, false, paamelding3);
		due7.setPaamelding(paamelding3);
		Due due8 = new Due("14-23", "Norsk Tomler", "Grønn", "Kort", false, false, true, paamelding3);

		Due due9 = new Due("15-23", "Engelsk Tippler", "Rød", "Standard", true, false, false, paamelding4);
		due9.setPaamelding(paamelding4);
		Due due10 = new Due("16-23", "Bergensprakt", "Gul", "Standard", true, false, false, paamelding4);
		due10.setPaamelding(paamelding4);
		Due due11 = new Due("17-23", "Fantastisk Flyver", "Oransje", "Lang", false, true, false, paamelding4);
		due11.setPaamelding(paamelding4);
		Due due12 = new Due("18-23", "Fantastisk Flyver", "Lilla", "Kort", false, false, true, paamelding4);

		Due due13 = new Due("19-23", "Bergensprakt", "Grå", "Standard", true, false, false, paamelding4);
		due13.setPaamelding(paamelding4);
		Due due14 = new Due("20-23", "Fantastisk Flyver", "Hvit", "Lang", false, true, false, paamelding4);
		due14.setPaamelding(paamelding4);
		Due due15 = new Due("21-23", "Engelsk Tippler", "Svart", "Kort", false, false, true, paamelding4);

		// Save additional Paamelding and Due instances
		paameldingService.manueltLeggTilPaamelding(paamelding1);
		paameldingService.manueltLeggTilPaamelding(paamelding2);
		paameldingService.manueltLeggTilPaamelding(paamelding3);
		paameldingService.manueltLeggTilPaamelding(paamelding4);

		dueService.leggTilDue(due1);
		dueService.leggTilDue(due2);
		dueService.leggTilDue(due3);
		dueService.leggTilDue(due4);
		dueService.leggTilDue(due5);
		dueService.leggTilDue(due6);
		dueService.leggTilDue(due7);
		dueService.leggTilDue(due8);
		dueService.leggTilDue(due9);
		dueService.leggTilDue(due10);
		dueService.leggTilDue(due11);
		dueService.leggTilDue(due12);
		dueService.leggTilDue(due13);
		dueService.leggTilDue(due14);
		dueService.leggTilDue(due15);
	}

	private void utstillingMedMangeDuer() {

		Bruker bruker = new Bruker("Mange", "Duer", "dududud 69", "mange@duer.com", "87654321",
				"Norsk Forening for Mange Duer");
		setPassword(bruker, "1234");
		bruker = brukerService.lagreBruker(bruker);

		Utstilling u = utstillingService.leggTilUtstilling(
				new Utstilling("Peristeri", "Bergen", "Test Mange Duer",
						"Dette er en test utstilling for å ha mange duer registrert",
						LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), LocalDate.now().minusDays(28),
						100.0));
		u = utstillingService.setAktivUtstilling(u.getId());
		Bruker dommer = new Bruker("Dommer for", "Mange duer", "Et sted", "dommer@pikksteri.no", "123456789",
				"Dommerforeningen", Rolle.DOMMER);
		DommerPaamelding dp = dommerService.lagreDommerPaamelding(dommer, u.getId(), "dommer");

		Paamelding p = new Paamelding(bruker, u);

		int    antallDuer = 126;
		Random random     = new Random();
		List<String> c = List.of(
				"Rød",
				"Blå",
				"Hvit",
				"kvit",
				"raud"
		);
		List<String> dr = List.of(
				"Rock Pigeon",
				"Common Wood Pigeon",
				"Eurasian Collared Dove",
				"Stock Dove",
				"Speckled Pigeon"
		);
		List<String> v = List.of(
				"Standard",
				"stardard",
				"Kort"
		);

		List<Due> duer = new ArrayList<>();

		for (int i = 0; i < antallDuer; i++) {
			Due due = new Due(i + "-23", dr.get(random.nextInt(dr.size())), c.get(random.nextInt(c.size())),
					v.get(random.nextInt(v.size())), random.nextBoolean(), random.nextBoolean(),
					random.nextBoolean(), p);
			due.setPaamelding(p);

			if (i % 1 == 0) {
				int score = 90 + random.nextInt(11);
				Bedommelse bedommelse = new Bedommelse(score, "God standard", "Fin form", "Ingen større feil");
				bedommelse.setDue(due);
				due.setBedommelse(bedommelse);
			}

			duer.add(due);
		}
		paameldingService.manueltLeggTilPaamelding(p);

		for (Due due : duer) {
			dueService.leggTilDue(due);
		}


		List<String> raser = dueService.hentRaserPaameldtUtstilling(u.getId()).reversed();
		utstillingService.oppdaterSorterteRaser(u.getId(), RaseStringHjelper.konverterTilString(raser));

		dommerService.fordelRaserTilDommer(dp.getId(), raser);

		utstillingService.genererBurnumre(u);


	}

}
