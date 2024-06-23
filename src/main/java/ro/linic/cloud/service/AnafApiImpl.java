package ro.linic.cloud.service;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.helger.commons.error.list.IErrorList;
import com.helger.ubl21.UBL21Marshaller;

import lombok.NonNull;
import lombok.extern.java.Log;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import ro.linic.cloud.component.I18n;
import ro.linic.cloud.mapper.InvoiceMapper;
import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.pojo.Party;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessages;
import ro.linic.cloud.pojo.anaf.AnafUploadResponseHeader;
import ro.linic.cloud.pojo.anaf.AnafUploadStateResponseHeader;

@Service
@Log
public class AnafApiImpl implements AnafApi {
	@Autowired private RestTemplate restTemplate;
	@Autowired private I18n i18n;
	
	@Value("${anaf.api.base.url:https://api.anaf.ro/test/FCTEL}")
	private String anafApiBaseUrl;
	
	private static void validateInvoice(final InvoiceType ublInvoice) {
		final IErrorList invoiceValidationResult = UBL21Marshaller.invoice().validate(ublInvoice);
        if (invoiceValidationResult.containsAtLeastOneError())
        	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, invoiceValidationResult.getAllTexts(LocaleContextHolder.getLocale()).toString());
	}
	
	@Override
	public ResponseEntity<AnafUploadResponseHeader> uploadInvoice(@NonNull final OAuth2AccessToken accessToken, @NonNull final Invoice invoice) {
		final String taxNumber = Optional.ofNullable(invoice.getAccountingSupplier())
				.map(Party::getTaxId)
				.map(taxId -> StringUtils.removeStartIgnoreCase(taxId, "RO"))
				.orElse("");
		
		final String urlTemplate = UriComponentsBuilder.fromHttpUrl(anafApiBaseUrl+"/rest/upload")
		        .queryParam("standard", "UBL")
		        .queryParam("cif", taxNumber)
		        .encode()
		        .toUriString();
		
		final HttpHeaders headers = createHeaders(accessToken);
        final InvoiceType ublInvoice = InvoiceMapper.INSTANCE.toUblInvoice(invoice);
        validateInvoice(ublInvoice);
		final String ublInvoiceXml = UBL21Marshaller.invoice().getAsString(ublInvoice);
		log.info(ublInvoiceXml);
		final HttpEntity<String> request = new HttpEntity<String>(ublInvoiceXml, headers);
		
		return restTemplate.exchange(urlTemplate, HttpMethod.POST, request,
				new ParameterizedTypeReference<AnafUploadResponseHeader>(){});
	}

	private HttpHeaders createHeaders(final OAuth2AccessToken accessToken) {
		final HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken.getTokenValue());
		return headers;
	}
	
	@Override
	public ResponseEntity<String> testOauth(@NonNull final OAuth2AccessToken accessToken, final String name) {
		final String urlTemplate = UriComponentsBuilder.fromHttpUrl("https://api.anaf.ro/TestOauth/jaxrs/hello")
		        .queryParam("name", name)
		        .encode()
		        .toUriString();
		
		final HttpHeaders headers = createHeaders(accessToken);
        final HttpEntity<String> request = new HttpEntity<String>(headers);
		return restTemplate.exchange(urlTemplate, HttpMethod.GET, request,
				new ParameterizedTypeReference<String>(){});
	}

	@Override
	public ResponseEntity<AnafUploadStateResponseHeader> checkInvoiceState(@NonNull final OAuth2AccessToken accessToken,
			@NonNull final String uploadIndex) {
		final String urlTemplate = UriComponentsBuilder.fromHttpUrl(anafApiBaseUrl+"/rest/stareMesaj")
		        .queryParam("id_incarcare", uploadIndex)
		        .encode()
		        .toUriString();
		
		final HttpHeaders headers = createHeaders(accessToken);
		final HttpEntity<String> request = new HttpEntity<String>(headers);
		return restTemplate.exchange(urlTemplate, HttpMethod.GET, request,
				new ParameterizedTypeReference<AnafUploadStateResponseHeader>(){});
	}
	
	@Override
	public ResponseEntity<byte[]> downloadResponse(@NonNull final OAuth2AccessToken accessToken, @NonNull final String downloadId) {
		final String urlTemplate = UriComponentsBuilder.fromHttpUrl(anafApiBaseUrl+"/rest/descarcare")
		        .queryParam("id", downloadId)
		        .encode()
		        .toUriString();
		
		final HttpHeaders headers = createHeaders(accessToken);
		final HttpEntity<String> request = new HttpEntity<String>(headers);
		return restTemplate.exchange(urlTemplate, HttpMethod.GET, request,
				new ParameterizedTypeReference<byte[]>(){});
	}
	
	@Override
	public ResponseEntity<AnafReceivedMessages> receivedMessages(@NonNull final OAuth2AccessToken accessToken, @NonNull final String cif,
			final int days) {
		if (days < 1 || days > 60)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, i18n.getMessage("error.anaf_api.days_not_in_range"));
		
		final String urlTemplate = UriComponentsBuilder.fromHttpUrl(anafApiBaseUrl+"/rest/listaMesajeFactura")
		        .queryParam("zile", days)
		        .queryParam("cif", cif)
		        .encode()
		        .toUriString();
		
		final HttpHeaders headers = createHeaders(accessToken);
		final HttpEntity<String> request = new HttpEntity<String>(headers);
		return restTemplate.exchange(urlTemplate, HttpMethod.GET, request,
				new ParameterizedTypeReference<AnafReceivedMessages>(){});
	}
}
