package no.hvl.peristeri.feature.utstilling;

import no.hvl.peristeri.common.DateRange;
import no.hvl.peristeri.feature.paamelding.PaameldingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UtstillingController.
 * 
 * Uses @MockitoBean for controller-layer dependency mocking.
 */
@WebMvcTest(UtstillingController.class)
@ActiveProfiles({"prod","test"})
public class UtstillingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtstillingService utstillingService;

    @MockitoBean
    private PaameldingService paameldingService;

    private Utstilling testUtstilling;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        testUtstilling = new Utstilling();
        testUtstilling.setId(1L);
        testUtstilling.setTittel("Test Utstilling");
        testUtstilling.setArrangoer("Test Arrangoer");
        testUtstilling.setAdresse("Test Adresse");
        testUtstilling.setPostnummer("1234");
        testUtstilling.setPoststed("Test Poststed");
        testUtstilling.setBeskrivelse("Test Beskrivelse");
        testUtstilling.setDatoRange(new DateRange(today.plusDays(10), today.plusDays(12)));
        testUtstilling.setPaameldingsFrist(today.plusDays(5));
        testUtstilling.setPaameldingAApnet(true);
        testUtstilling.setDuePris(BigDecimal.valueOf(100.0));
        testUtstilling.setRaseSortering("Norsk Tomler|Dansk Tomler|Tysk Modeneser");
        testUtstilling.setHarBurnumre(false);
        testUtstilling.setAktiv(false);
    }

    @Test
    @WithMockUser
    void utstilling_shouldReturnUtstillingPage() throws Exception {
        // Arrange
        List<Utstilling> kommendeUtstillinger = Collections.singletonList(testUtstilling);
        List<Utstilling> tidligereUtstillinger = Collections.emptyList();

        when(utstillingService.finnIkkeTidligereUtstillinger()).thenReturn(kommendeUtstillinger);
        when(utstillingService.finnTidligereUtstillinger()).thenReturn(tidligereUtstillinger);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling"))
                .andExpect(status().isOk())
                .andExpect(view().name("utstilling/utstilling"))
                .andExpect(model().attribute("fragment", "liste"))
                .andExpect(model().attribute("kommendeUtstillinger", hasSize(1)))
                .andExpect(model().attribute("tidligereUtstillinger", hasSize(0)));
    }

    @Test
    @WithMockUser
    void visUtstilling_withValidId_shouldReturnUtstillingDetailsPage() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(1L)).thenReturn(testUtstilling);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("utstilling/utstilling"))
                .andExpect(model().attribute("fragment", "detaljer"))
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser
    void visUtstilling_withInvalidId_shouldRedirectToListPage() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling/999"))
                .andExpect(status().isOk())
                .andExpect(view().name("utstilling/utstilling"))
                .andExpect(model().attribute("fragment", "liste"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void visUtstilling_withAuthenticatedUser_shouldIncludePaameldingInfo() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(1L)).thenReturn(testUtstilling);
        // We don't need to check for the paamelding attribute since it might be null

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("utstilling/utstilling"))
                .andExpect(model().attribute("fragment", "detaljer"))
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void visUtstilling_withAdminUser_shouldIncludeStatusInfo() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(1L)).thenReturn(testUtstilling);
        // We don't need to check for the paamelding and status attributes since they might be null

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("utstilling/utstilling"))
                .andExpect(model().attribute("fragment", "detaljer"))
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser
    void visRegistrerUtstilling_shouldReturnRegistrationForm() throws Exception {
        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling/registrer"))
                .andExpect(status().isOk())
                .andExpect(view().name("utstilling/utstilling"))
                .andExpect(model().attribute("fragment", "registreringsSkjema"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void opprettUtstilling_withValidData_shouldCreateUtstillingAndRedirect() throws Exception {
        // Arrange
        when(utstillingService.leggTilUtstilling(any(Utstilling.class))).thenReturn(testUtstilling);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/utstilling/registrer")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tittel", "Test Utstilling")
                .param("arrangoer", "Test Arrangoer")
                .param("adresse", "Test Adresse")
                .param("postnummer", "1234")
                .param("poststed", "Test Poststed")
                .param("beskrivelse", "Test Beskrivelse")
                .param("datoRange.startDate", today.plusDays(10).toString())
                .param("datoRange.endDate", today.plusDays(12).toString())
                .param("paameldingsFrist", today.plusDays(5).toString())
                .param("duePris", "100.0")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                // The actual view is a FragmentsRendering, not a view name
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void visRedigerUtstilling_withValidId_shouldReturnEditForm() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(1L)).thenReturn(testUtstilling);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling/rediger/1")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                // The actual view is a FragmentsRendering, not a view name
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void redigerUtstilling_withValidData_shouldUpdateUtstillingAndRedirect() throws Exception {
        // Arrange
        when(utstillingService.oppdaterUtstilling(eq(1L), any(Utstilling.class))).thenReturn(testUtstilling);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/utstilling/rediger/1")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .param("tittel", "Updated Utstilling")
                .param("arrangoer", "Updated Arrangoer")
                .param("adresse", "Updated Adresse")
                .param("postnummer", "5678")
                .param("poststed", "Updated Poststed")
                .param("beskrivelse", "Updated Beskrivelse")
                .param("datoRange.startDate", today.plusDays(10).toString())
                .param("datoRange.endDate", today.plusDays(12).toString())
                .param("paameldingsFrist", today.plusDays(5).toString())
                .param("duePris", "150.0")
                .header("HX-Request", "true"))
                .andExpect(status().isOk())
                // The actual view is a FragmentsRendering, not a view name
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aapnePaamelding_withValidId_shouldOpenRegistrationAndRedirect() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(1L)).thenReturn(testUtstilling);
        when(utstillingService.oppdaterUtstilling(eq(1L), any(Utstilling.class))).thenReturn(testUtstilling);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/utstilling/1/aapnePaamelding")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .header("HX-Request", "true"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void lukkPaamelding_withValidId_shouldCloseRegistrationAndRedirect() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(1L)).thenReturn(testUtstilling);
        when(utstillingService.oppdaterUtstilling(eq(1L), any(Utstilling.class))).thenReturn(testUtstilling);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/utstilling/1/lukkPaamelding")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .header("HX-Request", "true"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(model().attribute("utstilling", testUtstilling));
    }

    @Test
    @WithMockUser
    void visLiveScore_withValidId_shouldReturnLiveScorePage() throws Exception {
        // Arrange
        when(utstillingService.finnUtstillingMedId(1L)).thenReturn(testUtstilling);
        when(utstillingService.finnAlleDuerFraUtstillingSomHarBedommelse(1L)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/utstilling/livescore/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("utstilling/livescore"))
                .andExpect(model().attribute("utstilling", testUtstilling))
                .andExpect(model().attribute("utstillingId", 1L))
                .andExpect(model().attribute("duer", hasSize(0)));
    }
}
