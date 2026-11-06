package com.bank.cardenrichment;

import com.bank.cardenrichment.support.CardTransactionFixtures;
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
import tools.jackson.databind.json.JsonMapper;

import static com.bank.cardenrichment.support.CardTransactionFixtures.batchOfTescoAndUnknown;
import static com.bank.cardenrichment.support.CardTransactionFixtures.tescoPraha;
import static com.bank.cardenrichment.support.CardTransactionFixtures.withoutAcceptorName;
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
        mvc.perform(post("/api/v1/card-transactions/enrich", tescoPraha()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-1"))
                .andExpect(jsonPath("$.status").value("FOUND"))
                .andExpect(jsonPath("$.tapixHandle").value("$91vbcF7535E43615lq7bqQ"))
                .andExpect(jsonPath("$.merchant.name").value("Tesco"))
                .andExpect(jsonPath("$.shop.type").value("BRICKS"))
                .andExpect(jsonPath("$.shop.category.name").value("Groceries"))
                .andExpect(jsonPath("$.shop.address.city").value("Praha 5"))
                .andExpect(jsonPath("$.shop.coordinates.longitude").value(14.294543));

        tapix.verifySingleLookup("TESCO", "18052001", "POS");
    }

    @Test
    void enrichesBatchInOrder() throws Exception {
        mvc.perform(post("/api/v1/card-transactions/enrich/batch", batchOfTescoAndUnknown()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].transactionId").value("TX-1"))
                .andExpect(jsonPath("$.results[0].status").value("FOUND"))
                .andExpect(jsonPath("$.results[1].transactionId").value("TX-2"))
                .andExpect(jsonPath("$.results[1].status").value("UNSOLVED"))
                .andExpect(jsonPath("$.results[1].shop").doesNotExist());

        tapix.verifyBatchLookup();
    }

    @Test
    void rejectsTransactionWithoutAcceptorName() throws Exception {
        mvc.perform(post("/api/v1/card-transactions/enrich", withoutAcceptorName()))
                .andExpect(status().isBadRequest());

        tapix.verifyNoLookup();
    }

    @Test
    void returnsBadGatewayWhenTapixFails() throws Exception {
        tapix.stubSingleLookupFailure(500);

        mvc.perform(post("/api/v1/card-transactions/enrich", tescoPraha()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Tapix enrichment failed"));
    }

    private MockHttpServletRequestBuilder post(String url, Object body) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_card-enrichment:read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
    }
}
