package com.bank.cardenrichment;

import com.bank.cardenrichment.support.TapixMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

import static com.bank.cardenrichment.support.CardTransactionFixtures.tescoPraha;
import static com.bank.cardenrichment.support.CardTransactionFixtures.withoutDescription;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full chain: our API -> OAuth2 token -> Tapix (see {@link TapixMockServer}).
 * Incoming auth is not under test: requests carry a ready-made JWT with the right scope.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CardEnrichmentApiTest {

    /** Our endpoint; adjust if the path changed with the contract. */
    static final String ENRICH_PATH = "/api/v1/card-transactions/enrich";

    static final TapixMockServer tapix = new TapixMockServer();

    @Autowired
    MockMvc mvc;
    @Autowired
    JsonMapper json;

    @DynamicPropertySource
    static void tapixProperties(DynamicPropertyRegistry registry) {
        tapix.registerProperties(registry);
    }

    @BeforeEach
    void stubTapix() {
        tapix.stubHappyPath();
    }

    @AfterAll
    static void stopTapix() {
        tapix.stop();
    }

    @Test
    void enrichesOneTransaction() throws Exception {
        mvc.perform(enrich(tescoPraha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("found"))
                .andExpect(jsonPath("$.handle").value("$91vbcF7535E43615lq7bqQ"))
                .andExpect(jsonPath("$.customId").value("TX-1"))
                .andExpect(jsonPath("$.shop.type").value("bricks"))
                .andExpect(jsonPath("$.shop.category.name").value("Groceries"))
                .andExpect(jsonPath("$.shop.merchant.name").value("Tesco"))
                .andExpect(jsonPath("$.shop.location.address.city").value("Praha 5"))
                .andExpect(jsonPath("$.shop.location.coordinates.long").value(14.294543));

        tapix.verifySingleLookup("TESCO", "18052001", "POS");
    }

    @Test
    void returnsUnsolvedWhenTapixFindsNothing() throws Exception {
        tapix.stubLookupUnsolved();

        mvc.perform(enrich(tescoPraha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("unsolved"))
                .andExpect(jsonPath("$.shop").doesNotExist());
    }

    @Test
    void rejectsTransactionWithoutDescription() throws Exception {
        mvc.perform(enrich(withoutDescription()))
                .andExpect(status().isBadRequest());

        tapix.verifyNoLookup();
    }

    @Test
    void returnsBadGatewayWhenTapixFails() throws Exception {
        tapix.stubSingleLookupFailure(500);

        mvc.perform(enrich(tescoPraha()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Tapix enrichment failed"));
    }

    private MockHttpServletRequestBuilder enrich(Object body) {
        return MockMvcRequestBuilders.post(ENRICH_PATH)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_card-enrichment:read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
    }
}
