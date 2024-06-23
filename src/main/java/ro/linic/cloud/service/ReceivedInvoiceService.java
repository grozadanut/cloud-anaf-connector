package ro.linic.cloud.service;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

import ro.linic.cloud.entity.ReceivedMessage;

public interface ReceivedInvoiceService {
	void billReceived(OAuth2AccessToken anafAccessToken, ReceivedMessage message);
}
