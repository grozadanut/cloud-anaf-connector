package ro.linic.cloud.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessages;
import ro.linic.cloud.pojo.anaf.AnafUploadResponseHeader;
import ro.linic.cloud.pojo.anaf.AnafUploadStateResponseHeader;

public interface AnafApi {
	ResponseEntity<AnafUploadResponseHeader> uploadInvoice(OAuth2AccessToken accessToken, Invoice invoice);
	ResponseEntity<String> testOauth(OAuth2AccessToken accessToken, String name);
	ResponseEntity<AnafUploadStateResponseHeader> checkInvoiceState(OAuth2AccessToken accessToken, String uploadIndex);
	ResponseEntity<byte[]> downloadResponse(OAuth2AccessToken accessToken, String downloadId);
	/**
	 * @param cif taxNumber of the company you want the messages for
	 * @param days number of days you want to search for messages back in time, values between 1 and 60
	 * @return
	 */
	ResponseEntity<AnafReceivedMessages> receivedMessages(OAuth2AccessToken accessToken, String cif, int days);
}
