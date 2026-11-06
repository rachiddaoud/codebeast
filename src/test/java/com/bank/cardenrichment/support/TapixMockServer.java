package com.bank.cardenrichment.support;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

/**
 * Fake Tapix (API + OAuth2 token endpoint). Payloads live in {@code src/test/resources/tapix/*.json}.
 */
public class TapixMockServer {

    public static final String TOKEN = "test-token";

    private static final String TOKEN_PATH = "/auth/realms/tapix-prod/protocol/openid-connect/token";
    private static final String SINGLE_PATH = "/v6/shops/complete/findByCardTransaction";
    private static final String BATCH_PATH = "/v6/shops/complete/findByCardTransactionBatch";

    private final ClientAndServer server = startClientAndServer();

    public void registerProperties(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + server.getPort();
        registry.add("TAPIX_URL", () -> url);
        registry.add("TAPIX_AUTH_URL", () -> url);
        registry.add("TAPIX_CLIENT_ID", () -> "test-client");
        registry.add("TAPIX_CLIENT_SECRET", () -> "test-secret");
        registry.add("JWT_JWK_SET_URI", () -> url + "/unused");
    }

    /** Token OK, single lookup -> found, batch lookup -> [found, unsolved]. */
    public void stubHappyPath() {
        server.reset();
        server.when(request().withMethod("POST").withPath(TOKEN_PATH)).respond(json(200, "token"));
        server.when(request().withMethod("GET").withPath(SINGLE_PATH)).respond(json(200, "found"));
        server.when(request().withMethod("POST").withPath(BATCH_PATH))
                .respond(response().withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[" + read("found") + "," + read("unsolved") + "]"));
    }

    public void stubSingleLookupFailure(int status) {
        server.clear(request().withPath(SINGLE_PATH));
        server.when(request().withMethod("GET").withPath(SINGLE_PATH))
                .respond(response().withStatusCode(status));
    }

    public void verifySingleLookup(String description, String posId, String channelCode) {
        server.verify(request().withMethod("GET").withPath(SINGLE_PATH)
                        .withQueryStringParameters(
                                param("description", ".*" + description + ".*"),
                                param("posId", posId),
                                param("channelCode", channelCode),
                                param("refresh", "false"))
                        .withHeader("Authorization", "Bearer " + TOKEN),
                VerificationTimes.once());
    }

    public void verifyBatchLookup() {
        server.verify(request().withMethod("POST").withPath(BATCH_PATH)
                        .withHeader("Authorization", "Bearer " + TOKEN),
                VerificationTimes.once());
    }

    public void verifyNoLookup() {
        server.verify(request().withPath("/v6/.*"), VerificationTimes.exactly(0));
    }

    public void stop() {
        server.stop();
    }

    private static HttpResponse json(int status, String fixture) {
        return response().withStatusCode(status)
                .withHeader("Content-Type", "application/json")
                .withBody(read(fixture));
    }

    private static String read(String fixture) {
        try {
            return new ClassPathResource("tapix/" + fixture + ".json").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
