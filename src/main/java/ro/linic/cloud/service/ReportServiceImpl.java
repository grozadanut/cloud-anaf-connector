package ro.linic.cloud.service;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.NonNull;
import lombok.extern.java.Log;
import ro.linic.cloud.component.I18n;
import ro.linic.cloud.entity.CompanyOAuthToken;
import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.entity.ReportedInvoice;
import ro.linic.cloud.entity.ReportedInvoice.ReportState;
import ro.linic.cloud.mapper.AnafMessageMapper;
import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessages;
import ro.linic.cloud.pojo.anaf.AnafResponseError;
import ro.linic.cloud.pojo.anaf.AnafUploadResponseHeader;
import ro.linic.cloud.pojo.anaf.AnafUploadStateResponseHeader;
import ro.linic.cloud.repository.CompanyOAuthTokenRepository;
import ro.linic.cloud.repository.ReceivedMessageRepository;
import ro.linic.cloud.repository.ReportedInvoiceRepository;

@Service
@Transactional
@Log
public class ReportServiceImpl implements ReportService {
	@Autowired private I18n i18n;
	@Autowired private ReportedInvoiceRepository reportedInvoiceRepo;
	@Autowired private ReceivedMessageRepository receivedMessageRepo;
	@Autowired private CompanyOAuthTokenRepository companyOAuthTokenRepos;
	@Autowired private OAuth2AuthorizedClientService authorizedClientService;
	@Autowired private OAuth2AuthorizedClientManager authorizedClientManager;
	@Autowired private ReceivedInvoiceService receivedInvoiceService;
	@Autowired private AnafApi anafApi;
	
	@Value("${anaf.request.sleep-delay.seconds:2}")
	private Integer anafSleepDelay;
	
	@Override
	public ReportedInvoice reportInvoice(final int companyId, @NonNull final Invoice invoice) {
		if (invoice.getId() == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, i18n.getMessage("error.missing_id"));
		
		final Optional<ReportedInvoice> reportedInvoice = reportedInvoiceRepo.findById(invoice.getId());
		if (reportedInvoice.isPresent())
			validateReportState(reportedInvoice.get().getState());
		
		final OAuth2AccessToken anafAccessToken = findAnafAccessToken(companyId);
		final ResponseEntity<AnafUploadResponseHeader> uploadResult = anafApi.uploadInvoice(anafAccessToken, invoice);
		final ReportedInvoice reportToSave = mapResult(uploadResult);
		reportToSave.setInvoiceId(invoice.getId());
		return reportedInvoiceRepo.save(reportToSave);
	}
	
	private OAuth2AccessToken findAnafAccessToken(final int companyId) {
		final Optional<CompanyOAuthToken> companyOAuthToken = companyOAuthTokenRepos.findById(companyId);
		
		if (companyOAuthToken.isEmpty())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MessageFormat.format(i18n.getMessage("error.missing_anaf_token"), companyId));
		
		final OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
				companyOAuthToken.get().getClientRegistrationId(), companyOAuthToken.get().getPrincipalName());
		
		if (authorizedClient == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, MessageFormat.format(i18n.getMessage("error.missing_anaf_token"), companyId));
		
		final OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withAuthorizedClient(authorizedClient)
                .principal(companyOAuthToken.get().getPrincipalName())
                .build();

		return this.authorizedClientManager.authorize(authorizeRequest).getAccessToken();
	}

	private void validateReportState(final ReportState state) {
		if (ReportState.SENT.equals(state))
			throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, i18n.getMessage("error.already_reported"));
		else if (ReportState.WAITING_VALIDATION.equals(state))
			throw new ResponseStatusException(HttpStatus.TOO_EARLY, i18n.getMessage("error.waiting_validation"));
	}
	
	private ReportedInvoice mapResult(final ResponseEntity<AnafUploadResponseHeader> uploadResult) {
		final ReportedInvoice report = new ReportedInvoice();
		if (uploadResult.getStatusCode().is2xxSuccessful()) {
			if (uploadResult.getBody().isExecutionStatusOk()) {
				report.setState(ReportState.WAITING_VALIDATION);
				report.setUploadIndex(uploadResult.getBody().getUploadIndex());
			} else {
				report.setState(ReportState.UPLOAD_ERROR);
				report.setErrorMessage(uploadResult.getBody().getErrors().stream()
						.map(AnafResponseError::getMessage)
						.collect(Collectors.joining(System.lineSeparator())));
			}
		} else {
			report.setState(ReportState.UPLOAD_ERROR);
			report.setErrorMessage(uploadResult.getStatusCode().toString());
		}
		return report;
	}
	
	@Override
	public ResponseEntity<String> testOauth(final int companyId, final String name) {
		return anafApi.testOauth(findAnafAccessToken(companyId), name);
	}

	@Override
	public void checkReportedInvoicesState(final int companyId) {
		final OAuth2AccessToken anafAccessToken = findAnafAccessToken(companyId);
		
		reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION).forEach(awaitingInvoice -> {
			saveReportedInvoiceResult(anafAccessToken, awaitingInvoice);
			try {
				// throttle requests to limit ANAF quota as per official specs:
				// 100 Requests / 1 minute
				// 50 Spike arrest / 10 seconds
			    TimeUnit.SECONDS.sleep(anafSleepDelay);
			} catch (final InterruptedException ie) {
			    Thread.currentThread().interrupt();
			}
		});
	}
	
	private void saveReportedInvoiceResult(@NonNull final OAuth2AccessToken anafAccessToken, @NonNull final ReportedInvoice awaitingInvoice) {
		final ResponseEntity<AnafUploadStateResponseHeader> anafResponse = anafApi.checkInvoiceState(anafAccessToken,
				awaitingInvoice.getUploadIndex());
		
		if (!anafResponse.getStatusCode().is2xxSuccessful())
		{
			log.severe(MessageFormat.format("Check ANAF state failed for invoice {0} with returned status code {1}",
					awaitingInvoice.getInvoiceId(), anafResponse.getStatusCode()));
			return;
		}
		
		if (anafResponse.getBody().isStateOk()) {
			awaitingInvoice.setState(ReportState.SENT);
			awaitingInvoice.setDownloadId(anafResponse.getBody().getDownloadId());
			awaitingInvoice.setErrorMessage(null);
			reportedInvoiceRepo.save(awaitingInvoice);
		} else if (anafResponse.getBody().isStateNok()) {
			awaitingInvoice.setState(ReportState.REJECTED_INVALID);
			awaitingInvoice.setDownloadId(anafResponse.getBody().getDownloadId());
			awaitingInvoice.setErrorMessage(MessageFormat.format("Validarea facturii a esuat. Descarcati fisierul de erori cu id-ul {0}",
					anafResponse.getBody().getDownloadId()));
			reportedInvoiceRepo.save(awaitingInvoice);
		} else if (anafResponse.getBody().isStatePending()) {
			return;
		} else if (StringUtils.isNotEmpty(anafResponse.getBody().getState())) {
			awaitingInvoice.setState(ReportState.REJECTED_INVALID);
			awaitingInvoice.setErrorMessage(anafResponse.getBody().prettyErrorMessage());
			reportedInvoiceRepo.save(awaitingInvoice);
		} else {
			log.severe(MessageFormat.format("Check ANAF state failed for invoice {0} with error {1}", 
					awaitingInvoice.getInvoiceId(), anafResponse.getBody().prettyErrorMessage()));
		}
	}
	
	@Override
	public ResponseEntity<byte[]> downloadResponse(final int companyId, final String downloadId) {
		final OAuth2AccessToken anafAccessToken = findAnafAccessToken(companyId);
		return anafApi.downloadResponse(anafAccessToken, downloadId);
	}
	
	@Override
	public ResponseEntity<List<ReceivedMessage>> checkForReceivedMessages(final int companyId, final int days) {
		final OAuth2AccessToken anafAccessToken = findAnafAccessToken(companyId);
		final Optional<CompanyOAuthToken> companyOAuthToken = companyOAuthTokenRepos.findById(companyId);
		final ResponseEntity<AnafReceivedMessages> messagesResponse = anafApi.receivedMessages(anafAccessToken,
				companyOAuthToken.get().getTaxId(), days);
		
		if (!messagesResponse.getStatusCode().is2xxSuccessful())
			throw new ResponseStatusException(messagesResponse.getStatusCode());
		
		final AnafReceivedMessages receivedMessages = messagesResponse.getBody();
		if (StringUtils.isNotEmpty(receivedMessages.getError()))
		{
			if (receivedMessages.getError().equalsIgnoreCase(AnafReceivedMessages.NO_MESSAGES_ERROR))
				return ResponseEntity.ok(List.of());
			else if (receivedMessages.getError().equalsIgnoreCase(AnafReceivedMessages.TOO_MANY_MESSAGES_ERROR))
				throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, receivedMessages.getError());
			
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, receivedMessages.getError());
		}
		
		return ResponseEntity.ok(receivedMessages.getMessages().stream()
				.map(AnafMessageMapper.INSTANCE::toEntity)
				.map(receivedMessageRepo::save)
				.map(msg -> afterMessageSaved(anafAccessToken, msg))
				.collect(Collectors.toList()));
	}
	
	private ReceivedMessage afterMessageSaved(final OAuth2AccessToken anafAccessToken, final ReceivedMessage message) {
		if (message.getMessageType().equals(AnafReceivedMessageType.BILL_RECEIVED))
			receivedInvoiceService.billReceived(anafAccessToken, message);
		return message;
	}
}
