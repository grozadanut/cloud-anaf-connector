package ro.linic.cloud.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.linic.cloud.entity.CompanyOAuthToken;
import ro.linic.cloud.repository.CompanyOAuthTokenRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {
	@Autowired private CompanyOAuthTokenRepository companyOAuthTokenRepos;
	
	@GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal final OAuth2User principal) {
        return Collections.singletonMap("name", principal.getAttribute("name"));
    }
	
	@GetMapping("/register/{companyId}")
    public CompanyOAuthToken registerCompanyToken(@RegisteredOAuth2AuthorizedClient final OAuth2AuthorizedClient authorizedClient,
    		@PathVariable(name = "companyId") final Integer companyId, @RequestParam final String taxId) {
		final CompanyOAuthToken companyToken = new CompanyOAuthToken();
		companyToken.setCompanyId(companyId);
		companyToken.setTaxId(taxId);
		companyToken.setClientRegistrationId(authorizedClient.getClientRegistration().getRegistrationId());
		companyToken.setPrincipalName(authorizedClient.getPrincipalName());
        return companyOAuthTokenRepos.save(companyToken);
    }
}
