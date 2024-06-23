package ro.linic.cloud;

import java.time.Instant;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;

import ro.linic.cloud.entity.CompanyOAuthToken;

public class TestData {
	public static CompanyOAuthToken companyToken;
	public static OAuth2AccessToken accessToken;
	public static OAuth2AuthorizedClient authorizedClient;
	public static Authentication principal;
	
	public static void init() {
		companyTokenInit();
		accessTokenInit();
		authorizedClientInit();
		principalInit();
	}
	
	private static void companyTokenInit() {
		companyToken = new CompanyOAuthToken();
		companyToken.setCompanyId(1);
		companyToken.setTaxId("15487754");
		companyToken.setClientRegistrationId("anaf");
		companyToken.setPrincipalName("Danut");
	}
	
	private static void accessTokenInit() {
		accessToken = new OAuth2AccessToken(TokenType.BEARER, "abc123", Instant.now(), Instant.now().plusSeconds(3600));
	}
	
	private static void authorizedClientInit() {
		final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(companyToken.getClientRegistrationId())
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.clientId("clientId")
				.redirectUri("redirectUri")
				.authorizationUri("authorizationUri")
				.tokenUri("tokenUri")
				.build();
		
		authorizedClient = new OAuth2AuthorizedClient(clientRegistration, companyToken.getPrincipalName(), accessToken);
	}
	
	private static void principalInit() {
		principal = new AnonymousAuthenticationToken("key", companyToken.getPrincipalName(), AuthorityUtils.createAuthorityList("ROLE_USER"));
	}
}
