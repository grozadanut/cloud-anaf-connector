package ro.linic.cloud;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@TestConfiguration
@Profile("test")
public class TestSecurityConfig {
	@Bean @Primary
	public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(final ClientRegistrationRepository clientRegistrationRepository) {
	    return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
	}
}
