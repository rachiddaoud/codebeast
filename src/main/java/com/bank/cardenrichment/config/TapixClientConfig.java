package com.bank.cardenrichment.config;

import com.bank.cardenrichment.tapix.api.ShopsApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Tapix client: the generated {@link ShopsApi} HTTP interface, base URL from
 * {@code spring.http.serviceclient.tapix.*}, bearer token fetched (and refreshed) with the
 * {@code tapix} OAuth2 client credentials registration.
 */
@Configuration
@ImportHttpServices(group = "tapix", types = ShopsApi.class)
public class TapixClientConfig {

    @Bean
    OAuth2RestClientHttpServiceGroupConfigurer tapixOAuth2(ClientRegistrationRepository registrations,
                                                           OAuth2AuthorizedClientService authorizedClients) {
        // Service-based manager: works outside of an HTTP request (no user session involved)
        return OAuth2RestClientHttpServiceGroupConfigurer.from(
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients));
    }
}
