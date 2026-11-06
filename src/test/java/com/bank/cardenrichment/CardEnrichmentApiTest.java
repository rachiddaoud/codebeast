package com.bank.cardenrichment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full chain: our API -> OAuth2 token -> Tapix, against a MockServer.
 * Incoming auth is not under test: requests carry a ready-made JWT with the right scope.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CardEnrichmentApiTest {

    static final String TOKEN_PATH = "/auth/realms/tapix-prod/protocol/openid-connect/token";
    static final String SINGLE_PATH = "/v6/shops/complete/findByCardTransaction";
    static final String BATCH_PATH = "/v6/shops/complete/findByCardTransactionBatch";

    static final ClientAndServer tapix = startClientAndServer();

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void tapixProperties(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + tapix.getPort();
        registry.add("TAPIX_URL", () -> url);
        registry.add("TAPIX_AUTH_URL", () -> url);
        registry.add("TAPIX_CLIENT_ID", () -> "test-client");
        registry.add("TAPIX_CLIENT_SECRET", () -> "test-secret");
        registry.add("JWT_JWK_SET_URI", () -> url + "/unused");
    }

    @BeforeEach
    void reset() {
        tapix.reset();
        tapix.when(request().withMethod("POST").withPath(TOKEN_PATH))
                .respond(json(200, """
                        {"access_token":"test-token","token_type":"Bearer","expires_in":300}
                        """));
        tapix.when(request().withMethod("GET").withPath(SINGLE_PATH))
                .respond(json(200, FOUND));
        tapix.when(request().withMethod("POST").withPath(BATCH_PATH))
                .respond(json(200, "[" + FOUND + "," + UNSOLVED + "]"));
    }

    @AfterAll
    static void stop() {
        tapix.stop();
    }

    @Test
    void enrichesOneTransaction() throws Exception {
        mvc.perform(enrich("""
                        {"transactionId":"TX-1","acceptorName":"TESCO PRAHA ZLICIN","merchantId":"820014",
                         "terminalId":"18052001","city":"PRAHA","country":"CZ","mcc":"5411","channel":"POS"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TX-1"))
                .andExpect(jsonPath("$.status").value("FOUND"))
                .andExpect(jsonPath("$.tapixHandle").value("$91vbcF7535E43615lq7bqQ"))
                .andExpect(jsonPath("$.merchant.name").value("Tesco"))
                .andExpect(jsonPath("$.shop.type").value("BRICKS"))
                .andExpect(jsonPath("$.shop.category.name").value("Groceries"))
                .andExpect(jsonPath("$.shop.address.city").value("Praha 5"))
                .andExpect(jsonPath("$.shop.coordinates.longitude").value(14.294543));

        tapix.verify(request().withMethod("GET").withPath(SINGLE_PATH)
                        .withQueryStringParameters(
                                param("description", ".*TESCO.*"),
                                param("posId", "18052001"),
                                param("channelCode", "POS"),
                                param("refresh", "false"))
                        .withHeader("Authorization", "Bearer test-token"),
                VerificationTimes.once());
    }

    @Test
    void enrichesBatchInOrder() throws Exception {
        mvc.perform(post("/api/v1/card-transactions/enrich/batch")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_card-enrichment:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactions":[
                                  {"transactionId":"TX-1","acceptorName":"TESCO PRAHA ZLICIN"},
                                  {"transactionId":"TX-2","acceptorName":"UNKNOWN SHOP"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].transactionId").value("TX-1"))
                .andExpect(jsonPath("$.results[0].status").value("FOUND"))
                .andExpect(jsonPath("$.results[1].transactionId").value("TX-2"))
                .andExpect(jsonPath("$.results[1].status").value("UNSOLVED"))
                .andExpect(jsonPath("$.results[1].shop").doesNotExist());

        tapix.verify(request().withMethod("POST").withPath(BATCH_PATH)
                        .withHeader("Authorization", "Bearer test-token"),
                VerificationTimes.once());
    }

    @Test
    void rejectsTransactionWithoutAcceptorName() throws Exception {
        mvc.perform(enrich("""
                        {"transactionId":"TX-1"}
                        """))
                .andExpect(status().isBadRequest());

        tapix.verify(request().withPath("/v6/.*"), VerificationTimes.exactly(0));
    }

    @Test
    void returnsBadGatewayWhenTapixFails() throws Exception {
        tapix.clear(request().withPath(SINGLE_PATH));
        tapix.when(request().withMethod("GET").withPath(SINGLE_PATH))
                .respond(response().withStatusCode(500));

        mvc.perform(enrich("""
                        {"transactionId":"TX-1","acceptorName":"TESCO PRAHA ZLICIN"}
                        """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Tapix enrichment failed"));
    }

    private static MockHttpServletRequestBuilder enrich(String body) {
        return post("/api/v1/card-transactions/enrich")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_card-enrichment:read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static org.mockserver.model.HttpResponse json(int status, String body) {
        return response().withStatusCode(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static final String FOUND = """
            {"result":"found","handle":"$91vbcF7535E43615lq7bqQ","customId":"TX-1",
             "shop":{"uid":"MmPdedgnvjXiRZnBJzJQKb","type":"bricks","category":{"name":"Groceries"},
                     "tags":["Hyper-Supermarket"],"newTapixField":"ignored",
                     "location":{"address":{"street":"Skandinávská 144/25","city":"Praha 5","zip":"15500","country":"CZ"},
                                 "coordinates":{"type":"rooftop","lat":50.049689,"long":14.294543}},
                     "merchant":{"uid":"538VqZ4gn33tVJJWEYa8d3@gb","name":"Tesco","logo":"https://logo"}}}
            """;
    private static final String UNSOLVED = """
            {"result":"unsolved","handle":"!7GfEUnA8uF1aull3O89Dly","customId":"TX-2"}
            """;
}
