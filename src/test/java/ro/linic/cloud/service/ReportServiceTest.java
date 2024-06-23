package ro.linic.cloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import ro.linic.cloud.component.I18n;
import ro.linic.cloud.entity.CompanyOAuthToken;
import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.entity.ReportedInvoice;
import ro.linic.cloud.entity.ReportedInvoice.ReportState;
import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessages;
import ro.linic.cloud.pojo.anaf.AnafResponseError;
import ro.linic.cloud.pojo.anaf.AnafUploadResponseHeader;
import ro.linic.cloud.pojo.anaf.AnafUploadStateResponseHeader;
import ro.linic.cloud.repository.CompanyOAuthTokenRepository;
import ro.linic.cloud.repository.ReceivedMessageRepository;
import ro.linic.cloud.repository.ReportedInvoiceRepository;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

	@Mock private ReportedInvoiceRepository reportedInvoiceRepo;
	@Mock private ReceivedMessageRepository receivedMessageRepo;
	@Mock private CompanyOAuthTokenRepository companyOAuthTokenRepos;
	@Mock private OAuth2AuthorizedClientService authorizedClientService;
	@Mock private OAuth2AuthorizedClientManager authorizedClientManager;
	@Mock private AnafApi anafApi;
	@Mock private ReceivedInvoiceService receivedInvoiceService;
	@Mock private I18n i18n;
	@InjectMocks private ReportServiceImpl reportService;
	
	private CompanyOAuthToken companyToken;
	private OAuth2AccessToken accessToken;
	private OAuth2AuthorizedClient authorizedClient;
	
	@BeforeEach
	public void setup() {
		ReflectionTestUtils.setField(reportService, "anafSleepDelay", 0);
		companyToken = new CompanyOAuthToken();
		companyToken.setCompanyId(1);
		companyToken.setTaxId("15482531");
		companyToken.setClientRegistrationId("anaf");
		companyToken.setPrincipalName("Danut");
		
		accessToken = new OAuth2AccessToken(TokenType.BEARER, "abc123", Instant.now(), Instant.now().plusSeconds(3600));
		final ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(companyToken.getClientRegistrationId())
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.clientId("clientId")
				.redirectUri("redirectUri")
				.authorizationUri("authorizationUri")
				.tokenUri("tokenUri")
				.build();
		
		authorizedClient = new OAuth2AuthorizedClient(clientRegistration, companyToken.getPrincipalName(), accessToken);
		lenient().when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
	}
	
	@Test
	public void givenNullInvoiceId_whenReportInvoice_thenThrowException() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(null);
		when(i18n.getMessage("error.missing_id")).thenReturn("ID is mandatory!");
		
		// when
		assertThatThrownBy(() -> reportService.reportInvoice(1, invoice))
		// then
		.isInstanceOf(ResponseStatusException.class)
		.hasMessage("400 BAD_REQUEST \"ID is mandatory!\"");
	}
	
	@Test
	public void givenInvoiceIsReported_whenReportInvoice_thenThrowException() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		final ReportedInvoice sentInvoice = new ReportedInvoice();
		sentInvoice.setInvoiceId(1L);
		sentInvoice.setState(ReportState.SENT);
		
		when(i18n.getMessage("error.already_reported")).thenReturn("Invoice already reported!");
		when(reportedInvoiceRepo.findById(1L)).thenReturn(Optional.of(sentInvoice));
		
		// when
		assertThatThrownBy(() -> reportService.reportInvoice(1, invoice))
		// then
		.isInstanceOf(ResponseStatusException.class)
		.hasMessage("208 ALREADY_REPORTED \"Invoice already reported!\"");
	}

	@Test
	public void givenWaitingValidation_whenReportInvoice_thenThrowException() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		final ReportedInvoice reportedInvoice = new ReportedInvoice();
		reportedInvoice.setInvoiceId(1L);
		reportedInvoice.setState(ReportState.WAITING_VALIDATION);
		
		when(i18n.getMessage("error.waiting_validation")).thenReturn("Invoice waiting upload validation!");
		when(reportedInvoiceRepo.findById(1L)).thenReturn(Optional.of(reportedInvoice));
		
		// when
		assertThatThrownBy(() -> reportService.reportInvoice(1, invoice))
		// then
		.isInstanceOf(ResponseStatusException.class)
		.hasMessage("425 TOO_EARLY \"Invoice waiting upload validation!\"");
	}
	
	@Test
	public void givenCompanyOAuthTokenIsMissing_whenReportInvoice_thenThrowException() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.empty());
		when(i18n.getMessage("error.missing_anaf_token")).thenReturn("No ANAF OAuth token found for company with id {0}!");
		
		// when
		assertThatThrownBy(() -> reportService.reportInvoice(1, invoice))
		// then
		.isInstanceOf(ResponseStatusException.class)
		.hasMessage("401 UNAUTHORIZED \"No ANAF OAuth token found for company with id 1!\"");
	}
	
	@Test
	public void givenAuthorizedClientIsMissing_whenReportInvoice_thenThrowException() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(null);
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(i18n.getMessage("error.missing_anaf_token")).thenReturn("No ANAF OAuth token found for company with id {0}!");
		
		// when
		assertThatThrownBy(() -> reportService.reportInvoice(1, invoice))
		// then
		.isInstanceOf(ResponseStatusException.class)
		.hasMessage("401 UNAUTHORIZED \"No ANAF OAuth token found for company with id 1!\"");
	}
	
	@Test
	public void givenHasUploadError_whenReportInvoice_thenResendAndSaveResult() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		
		final ReportedInvoice reportedInvoice = new ReportedInvoice();
		reportedInvoice.setInvoiceId(1L);
		reportedInvoice.setState(ReportState.UPLOAD_ERROR);
		
		final AnafUploadResponseHeader anafResponseHeader = new AnafUploadResponseHeader();
		anafResponseHeader.setExecutionStatus("0");
		anafResponseHeader.setUploadIndex("1234");
		
		when(anafApi.uploadInvoice(accessToken, invoice)).thenReturn(new ResponseEntity<>(anafResponseHeader, HttpStatus.OK));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		when(reportedInvoiceRepo.findById(1L)).thenReturn(Optional.of(reportedInvoice));
		
		// when
		final ReportedInvoice report = reportService.reportInvoice(1, invoice);
		
		// then
		assertThat(report.getInvoiceId()).isEqualTo(1L);
		assertThat(report.getState()).isEqualTo(ReportState.WAITING_VALIDATION);
		assertThat(report.getUploadIndex()).isEqualTo("1234");
		assertThat(report.getErrorMessage()).isNull();
	}
	
	@Test
	public void givenHasRejectedInvalid_whenReportInvoice_thenResendAndSaveResult() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		
		final ReportedInvoice reportedInvoice = new ReportedInvoice();
		reportedInvoice.setInvoiceId(1L);
		reportedInvoice.setState(ReportState.REJECTED_INVALID);
		
		final AnafUploadResponseHeader anafResponseHeader = new AnafUploadResponseHeader();
		anafResponseHeader.setExecutionStatus("0");
		anafResponseHeader.setUploadIndex("1234");
		
		when(anafApi.uploadInvoice(accessToken, invoice)).thenReturn(new ResponseEntity<>(anafResponseHeader, HttpStatus.OK));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		when(reportedInvoiceRepo.findById(1L)).thenReturn(Optional.of(reportedInvoice));
		
		// when
		final ReportedInvoice report = reportService.reportInvoice(1, invoice);
		
		// then
		assertThat(report.getInvoiceId()).isEqualTo(1L);
		assertThat(report.getState()).isEqualTo(ReportState.WAITING_VALIDATION);
		assertThat(report.getUploadIndex()).isEqualTo("1234");
		assertThat(report.getErrorMessage()).isNull();
	}
	
	@Test
	public void givenInvoiceNotReported_whenReportInvoice_thenReportAndSaveResult() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		
		final AnafUploadResponseHeader anafResponseHeader = new AnafUploadResponseHeader();
		anafResponseHeader.setExecutionStatus("0");
		anafResponseHeader.setUploadIndex("1234");
		
		when(anafApi.uploadInvoice(accessToken, invoice)).thenReturn(new ResponseEntity<>(anafResponseHeader, HttpStatus.OK));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		
		// when
		final ReportedInvoice report = reportService.reportInvoice(1, invoice);
		
		// then
		assertThat(report.getInvoiceId()).isEqualTo(1L);
		assertThat(report.getState()).isEqualTo(ReportState.WAITING_VALIDATION);
		assertThat(report.getUploadIndex()).isEqualTo("1234");
		assertThat(report.getErrorMessage()).isNull();
	}
	
	@Test
	public void givenAnafUploadFails_whenReportInvoice_thenSaveErrorMessage() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		
		when(anafApi.uploadInvoice(accessToken, invoice)).thenReturn(new ResponseEntity<>((AnafUploadResponseHeader)null, HttpStatus.INTERNAL_SERVER_ERROR));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		
		// when
		final ReportedInvoice report = reportService.reportInvoice(1, invoice);
		
		// then
		assertThat(report.getInvoiceId()).isEqualTo(1L);
		assertThat(report.getState()).isEqualTo(ReportState.UPLOAD_ERROR);
		assertThat(report.getUploadIndex()).isNull();
		assertThat(report.getErrorMessage()).isEqualTo("500 INTERNAL_SERVER_ERROR");
	}
	
	@Test
	public void givenAnafUploadReturnsErrors_whenReportInvoice_thenSaveErrorMessage() {
		// given
		final Invoice invoice = new Invoice();
		invoice.setId(1L);
		
		final AnafUploadResponseHeader anafResponseHeader = new AnafUploadResponseHeader();
		anafResponseHeader.setExecutionStatus("1");
		anafResponseHeader.setErrors(List.of(new AnafResponseError("Not permitted")));
		
		when(anafApi.uploadInvoice(accessToken, invoice)).thenReturn(new ResponseEntity<>(anafResponseHeader, HttpStatus.OK));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		
		// when
		final ReportedInvoice report = reportService.reportInvoice(1, invoice);
		
		// then
		assertThat(report.getInvoiceId()).isEqualTo(1L);
		assertThat(report.getState()).isEqualTo(ReportState.UPLOAD_ERROR);
		assertThat(report.getUploadIndex()).isNull();
		assertThat(report.getErrorMessage()).isEqualTo("Not permitted");
	}
	
	@Test
	public void givenCompanyOAuthTokenIsMissing_whenCheckRepInvState_thenThrowException() {
		// given
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.empty());
		when(i18n.getMessage("error.missing_anaf_token")).thenReturn("No ANAF OAuth token found for company with id {0}!");
		
		// when
		assertThatThrownBy(() -> reportService.checkReportedInvoicesState(1))
		// then
		.isInstanceOf(ResponseStatusException.class)
		.hasMessage("401 UNAUTHORIZED \"No ANAF OAuth token found for company with id 1!\"");
	}
	
	@Test
	public void givenAnafStatusCodeIsNotOk_whenCheckRepInvState_thenSkipCheck() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.badRequest().build());
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		verify(reportedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenValidInvoice_whenCheckRepInvState_thenSaveDownloadIdAndState() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setState("ok");
		anafResponseHeader.setDownloadId("1234");
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		final ArgumentCaptor<ReportedInvoice> invCaptor = ArgumentCaptor.forClass(ReportedInvoice.class);
		verify(reportedInvoiceRepo).save(invCaptor.capture());
		final ReportedInvoice savedRepInv = invCaptor.getValue();
		
		assertThat(savedRepInv.getInvoiceId()).isEqualTo(1L);
		assertThat(savedRepInv.getState()).isEqualTo(ReportState.SENT);
		assertThat(savedRepInv.getUploadIndex()).isEqualTo(dbRepInv.getUploadIndex());
		assertThat(savedRepInv.getDownloadId()).isEqualTo("1234");
		assertThat(savedRepInv.getErrorMessage()).isNullOrEmpty();
	}
	
	@Test
	public void givenInvalidInvoice_whenCheckRepInvState_thenSaveDownloadIdAndState() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setState("nok");
		anafResponseHeader.setDownloadId("4321");
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		final ArgumentCaptor<ReportedInvoice> invCaptor = ArgumentCaptor.forClass(ReportedInvoice.class);
		verify(reportedInvoiceRepo).save(invCaptor.capture());
		final ReportedInvoice savedRepInv = invCaptor.getValue();
		
		assertThat(savedRepInv.getInvoiceId()).isEqualTo(1L);
		assertThat(savedRepInv.getState()).isEqualTo(ReportState.REJECTED_INVALID);
		assertThat(savedRepInv.getUploadIndex()).isEqualTo(dbRepInv.getUploadIndex());
		assertThat(savedRepInv.getDownloadId()).isEqualTo("4321");
		assertThat(savedRepInv.getErrorMessage()).isEqualTo("Validarea facturii a esuat. Descarcati fisierul de erori cu id-ul 4321");
	}
	
	@Test
	public void givenValidationIsPending_whenCheckRepInvState_thenSkipCheck() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setState("in prelucrare");
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		verify(reportedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenXmlInvalid_whenCheckRepInvState_thenSaveErrorState() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setState("XML cu erori nepreluat de sistem");
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		when(reportedInvoiceRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		final ArgumentCaptor<ReportedInvoice> invCaptor = ArgumentCaptor.forClass(ReportedInvoice.class);
		verify(reportedInvoiceRepo).save(invCaptor.capture());
		final ReportedInvoice savedRepInv = invCaptor.getValue();
		
		assertThat(savedRepInv.getInvoiceId()).isEqualTo(1L);
		assertThat(savedRepInv.getState()).isEqualTo(ReportState.REJECTED_INVALID);
		assertThat(savedRepInv.getUploadIndex()).isEqualTo(dbRepInv.getUploadIndex());
		assertThat(savedRepInv.getDownloadId()).isNull();
		assertThat(savedRepInv.getErrorMessage()).isEqualTo("XML cu erori nepreluat de sistem");
	}
	
	@Test
	public void givenUnauthorizedIndex_whenCheckRepInvState_thenSkipCheck() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setErrors(List.of(new AnafResponseError("Nu aveti dreptul de inteorgare pentru id_incarcare=3842")));
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		verify(reportedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenUnauthorizedTaxId_whenCheckRepInvState_thenSkipCheck() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setErrors(List.of(new AnafResponseError("Nu exista niciun CIF petru care sa aveti drept")));
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		verify(reportedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenInvalidUploadIndex_whenCheckRepInvState_thenSkipCheck() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("aaa");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setErrors(List.of(new AnafResponseError("Id_incarcare introdus= aaa nu este un numar intreg")));
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		verify(reportedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenInvoiceMissing_whenCheckRepInvState_thenSkipCheck() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("15000");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setErrors(List.of(new AnafResponseError("Nu exista factura cu id_incarcare= 15000")));
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		verify(reportedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenQuotaExceeded_whenCheckRepInvState_thenSkipCheck() {
		// given
		final ReportedInvoice dbRepInv = new ReportedInvoice();
		dbRepInv.setInvoiceId(1L);
		dbRepInv.setState(ReportState.WAITING_VALIDATION);
		dbRepInv.setUploadIndex("3842");
		
		final AnafUploadStateResponseHeader anafResponseHeader = new AnafUploadStateResponseHeader();
		anafResponseHeader.setErrors(List.of(new AnafResponseError("S-au facut deja 20 descarcari de mesaj in cursul zilei")));
		
		when(anafApi.checkInvoiceState(accessToken, dbRepInv.getUploadIndex())).thenReturn(ResponseEntity.ok(anafResponseHeader));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		when(reportedInvoiceRepo.findByState(ReportState.WAITING_VALIDATION)).thenReturn(List.of(dbRepInv));
		
		// when
		reportService.checkReportedInvoicesState(1);
		
		// then
		verify(reportedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenNoMessagesSaved_whenCheckForReceivedMessages_thenSaveAllMessages() {
		// given
		final AnafReceivedMessage billSent = new AnafReceivedMessage("3001503294", LocalDateTime.of(2022, 11, 1, 13, 36),
				companyToken.getTaxId(), "5001131297", "Factura cu id_incarcare=5001131297 emisa de cif_emitent="+companyToken.getTaxId()+" pentru cif_beneficiar=3",
    			AnafReceivedMessageType.BILL_SENT);
    	final AnafReceivedMessage billReceived = new AnafReceivedMessage("3009239535", LocalDateTime.of(2024, 1, 25, 14, 36),
    			companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
    	final AnafReceivedMessage billErrors = new AnafReceivedMessage("3001293434", LocalDateTime.of(2022, 11, 1, 14, 15),
    			companyToken.getTaxId(), "5001130147", "Erori de validare identificate la factura primita cu id_incarcare=5001130147",
    			AnafReceivedMessageType.BILL_ERRORS);
		final AnafReceivedMessages anafReceivedMessages = new AnafReceivedMessages();
		anafReceivedMessages.setMessages(List.of(billReceived, billSent, billErrors));
		
		when(anafApi.receivedMessages(accessToken, companyToken.getTaxId(), 60)).thenReturn(ResponseEntity.ok(anafReceivedMessages));
		when(receivedMessageRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		
		// when
		final ResponseEntity<List<ReceivedMessage>> savedMessages = reportService.checkForReceivedMessages(companyToken.getCompanyId(), 60);
		
		// then
		final ArgumentCaptor<ReceivedMessage> saveCaptor = ArgumentCaptor.forClass(ReceivedMessage.class);
		verify(receivedMessageRepo, times(3)).save(saveCaptor.capture());
		final List<ReceivedMessage> capturedMessages = saveCaptor.getAllValues();
		
		final ReceivedMessage savedBillSent = new ReceivedMessage(Long.parseLong(billSent.getId()), billSent.getCreationDate(),
				companyToken.getTaxId(), billSent.getUploadIndex(), billSent.getDetails(), AnafReceivedMessageType.BILL_SENT);
		final ReceivedMessage savedBillReceived = new ReceivedMessage(Long.parseLong(billReceived.getId()), billReceived.getCreationDate(),
				companyToken.getTaxId(), billReceived.getUploadIndex(), billReceived.getDetails(), AnafReceivedMessageType.BILL_RECEIVED);
		final ReceivedMessage savedBillErrors = new ReceivedMessage(Long.parseLong(billErrors.getId()), billErrors.getCreationDate(),
				companyToken.getTaxId(), billErrors.getUploadIndex(), billErrors.getDetails(), AnafReceivedMessageType.BILL_ERRORS);
		
		assertThat(savedMessages.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(savedMessages.getBody()).containsExactlyInAnyOrder(savedBillSent, savedBillReceived, savedBillErrors);
		assertThat(capturedMessages).containsExactlyInAnyOrder(savedBillSent, savedBillReceived, savedBillErrors);
	}
	
	@Test
	public void givenSomeMessagesSaved_whenCheckForReceivedMessages_thenSaveAndUpdateMessages() {
		// given
		final AnafReceivedMessage billSent = new AnafReceivedMessage("3001503294", LocalDateTime.of(2022, 11, 1, 13, 36),
				companyToken.getTaxId(), "5001131297", "Factura cu id_incarcare=5001131297 emisa de cif_emitent="+companyToken.getTaxId()+" pentru cif_beneficiar=3",
    			AnafReceivedMessageType.BILL_SENT);
    	final AnafReceivedMessage billReceived = new AnafReceivedMessage("3009239535", LocalDateTime.of(2024, 1, 25, 14, 36),
    			companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
    	final AnafReceivedMessage billErrors = new AnafReceivedMessage("3001293434", LocalDateTime.of(2022, 11, 1, 14, 15),
    			companyToken.getTaxId(), "5001130147", "Erori de validare identificate la factura primita cu id_incarcare=5001130147",
    			AnafReceivedMessageType.BILL_ERRORS);
		final AnafReceivedMessages anafReceivedMessages = new AnafReceivedMessages();
		anafReceivedMessages.setMessages(List.of(billReceived, billSent, billErrors));
		
		final ReceivedMessage savedBillSent = new ReceivedMessage(Long.parseLong(billSent.getId()), billSent.getCreationDate(),
				companyToken.getTaxId(), billSent.getUploadIndex(), billSent.getDetails(), AnafReceivedMessageType.BILL_SENT);
		final ReceivedMessage savedBillReceived = new ReceivedMessage(Long.parseLong(billReceived.getId()), billReceived.getCreationDate(),
				companyToken.getTaxId(), billReceived.getUploadIndex(), billReceived.getDetails(), AnafReceivedMessageType.BILL_RECEIVED);
		final ReceivedMessage savedBillErrors = new ReceivedMessage(Long.parseLong(billErrors.getId()), billErrors.getCreationDate(),
				companyToken.getTaxId(), billErrors.getUploadIndex(), billErrors.getDetails(), AnafReceivedMessageType.BILL_ERRORS);
		
		when(anafApi.receivedMessages(accessToken, companyToken.getTaxId(), 60)).thenReturn(ResponseEntity.ok(anafReceivedMessages));
		when(receivedMessageRepo.save(any())).then(AdditionalAnswers.returnsFirstArg());
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		
		// when
		final ResponseEntity<List<ReceivedMessage>> savedMessages = reportService.checkForReceivedMessages(companyToken.getCompanyId(), 60);
		
		// then
		final ArgumentCaptor<ReceivedMessage> saveCaptor = ArgumentCaptor.forClass(ReceivedMessage.class);
		verify(receivedMessageRepo, times(3)).save(saveCaptor.capture());
		final List<ReceivedMessage> capturedMessages = saveCaptor.getAllValues();
		
		assertThat(savedMessages.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(savedMessages.getBody()).containsExactlyInAnyOrder(savedBillSent, savedBillReceived, savedBillErrors);
		assertThat(capturedMessages).containsExactlyInAnyOrder(savedBillSent, savedBillReceived, savedBillErrors);
	}
	
	@Test
	public void givenNoMessagesFound_whenCheckForReceivedMessages_thenDoNothing() {
		// given
		final AnafReceivedMessages anafReceivedMessages = new AnafReceivedMessages();
		anafReceivedMessages.setError(AnafReceivedMessages.NO_MESSAGES_ERROR);
		
		when(anafApi.receivedMessages(accessToken, companyToken.getTaxId(), 60)).thenReturn(ResponseEntity.ok(anafReceivedMessages));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		
		// when
		final ResponseEntity<List<ReceivedMessage>> savedMessages = reportService.checkForReceivedMessages(companyToken.getCompanyId(), 60);
		
		// then
		verify(receivedMessageRepo, never()).save(any());
		
		assertThat(savedMessages.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(savedMessages.getBody()).isEmpty();
	}
	
	@Test
	public void givenTooManyMessagesError_whenCheckForReceivedMessages_thenThrowException() {
		// given
		final AnafReceivedMessages anafReceivedMessages = new AnafReceivedMessages();
		anafReceivedMessages.setError(AnafReceivedMessages.TOO_MANY_MESSAGES_ERROR);
		
		when(anafApi.receivedMessages(accessToken, companyToken.getTaxId(), 60)).thenReturn(ResponseEntity.ok(anafReceivedMessages));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		
		// when
		// then
		final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> reportService.checkForReceivedMessages(companyToken.getCompanyId(), 60));
		assertThat(ex.getMessage()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE + " \"" + AnafReceivedMessages.TOO_MANY_MESSAGES_ERROR+"\"");
	}
	
	@Test
	public void givenErrorReturned_whenCheckForReceivedMessages_thenThrowException() {
		// given
		final AnafReceivedMessages anafReceivedMessages = new AnafReceivedMessages();
		anafReceivedMessages.setError("Some other custom error happened! eg: credentials error");
		
		when(anafApi.receivedMessages(accessToken, companyToken.getTaxId(), 60)).thenReturn(ResponseEntity.ok(anafReceivedMessages));
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		
		// when
		// then
		final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> reportService.checkForReceivedMessages(companyToken.getCompanyId(), 60));
		assertThat(ex.getMessage()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE + " \"" + anafReceivedMessages.getError()+"\"");
	}
	
	@Test
	public void givenStatusCodeNotOK_whenCheckForReceivedMessages_thenThrowException() {
		// given
		when(anafApi.receivedMessages(accessToken, companyToken.getTaxId(), 60)).thenReturn(ResponseEntity.badRequest().build());
		when(companyOAuthTokenRepos.findById(1)).thenReturn(Optional.of(companyToken));
		when(authorizedClientService.loadAuthorizedClient(companyToken.getClientRegistrationId(), companyToken.getPrincipalName()))
		.thenReturn(authorizedClient);
		
		// when
		// then
		final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> reportService.checkForReceivedMessages(companyToken.getCompanyId(), 60));
		assertThat(ex.getMessage()).isEqualTo(HttpStatus.BAD_REQUEST.toString());
	}
}
