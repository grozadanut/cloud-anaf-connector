package ro.linic.cloud.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import ro.linic.cloud.component.AnafAuthorizationRequestResolver;
import ro.linic.cloud.component.AnafRequestEntityConverter;
import ro.linic.cloud.component.LinicOAuth2UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Autowired private ClientRegistrationRepository clientRegistrationRepository;

	@Bean
	public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
		http
		.authorizeHttpRequests()
		.requestMatchers("/auth/**")
		.authenticated()
		.anyRequest()
		.permitAll()
		.and()
		.oauth2Login()
			.tokenEndpoint().accessTokenResponseClient(accessTokenResponseClient())
			.and()
			.authorizationEndpoint().authorizationRequestResolver(new AnafAuthorizationRequestResolver(this.clientRegistrationRepository));
		
		http.csrf().disable();
		return http.build();
	}
	
	@Bean
	public OAuth2AuthorizedClientService oAuth2AuthorizedClientService
	        (final JdbcOperations jdbcOperations, final ClientRegistrationRepository clientRegistrationRepository) {
	    return new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
	}
	
	@Bean
	public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
	    return new LinicOAuth2UserService();
	}
	
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        final DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient(); 
        accessTokenResponseClient.setRequestEntityConverter(new AnafRequestEntityConverter()); 
        return accessTokenResponseClient;
    }
}
