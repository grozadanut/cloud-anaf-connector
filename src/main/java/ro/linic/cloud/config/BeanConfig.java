package ro.linic.cloud.config;

import java.util.Locale;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
@EnableScheduling
public class BeanConfig {
	@Bean
    public LocaleResolver localeResolver() {
        final AcceptHeaderLocaleResolver slr = new AcceptHeaderLocaleResolver();
        slr.setDefaultLocale(Locale.forLanguageTag("ro"));
        return slr;
    }
	
	@Bean
	public RestTemplate restTemplate(final RestTemplateBuilder builder) {
	    return builder.build();
	}
	
	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
	        final ClientRegistrationRepository clientRegistrationRepository,
	        final OAuth2AuthorizedClientService authorizedClientService) {

	    final OAuth2AuthorizedClientManager authorizedClientManager =
	            new AuthorizedClientServiceOAuth2AuthorizedClientManager(
	                    clientRegistrationRepository, authorizedClientService);

	    return authorizedClientManager;
	}
}
