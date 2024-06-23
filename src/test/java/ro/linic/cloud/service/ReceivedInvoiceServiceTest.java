package ro.linic.cloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import ro.linic.cloud.TestData;
import ro.linic.cloud.entity.ReceivedCreditNote;
import ro.linic.cloud.entity.ReceivedInvoice;
import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;
import ro.linic.cloud.repository.ReceivedCreditNoteRepository;
import ro.linic.cloud.repository.ReceivedInvoiceRepository;

@ExtendWith(MockitoExtension.class)
public class ReceivedInvoiceServiceTest {
	@Mock private ReceivedInvoiceRepository receivedInvoiceRepo;
	@Mock private ReceivedCreditNoteRepository receivedCreditNoteRepo;
	@Mock private AnafApi anafApi;
	@InjectMocks private ReceivedInvoiceServiceImpl receivedInvoiceService;
	
	@BeforeEach
	public void setup() throws Exception {
		TestData.init();
	}
	
	@Test
	public void givenWrongMessageType_whenBillReceived_thenThrowException() {
		// given
		final ReceivedMessage billSent = new ReceivedMessage(3001503294L, LocalDateTime.of(2022, 11, 1, 13, 36),
				TestData.companyToken.getTaxId(), "5001131297", "Factura cu id_incarcare=5001131297 emisa de cif_emitent="+TestData.companyToken.getTaxId()+" pentru cif_beneficiar=3",
    			AnafReceivedMessageType.BILL_SENT);
		
		// when
		// then
		final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> receivedInvoiceService.billReceived(TestData.accessToken, billSent));
		assertThat(ex.getMessage()).contains(HttpStatus.BAD_REQUEST.toString());
	}
	
	@Test
	public void givenInvoiceExists_whenBillReceived_thenDoNothing() {
		// given
		final ReceivedMessage billReceived = new ReceivedMessage(3009239535L, LocalDateTime.now().minusDays(10),
				TestData.companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+TestData.companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
		final ReceivedInvoice receivedInvoice = new ReceivedInvoice(billReceived.getId(), billReceived.getUploadIndex(), null, null, null, null);
		
		when(receivedInvoiceRepo.findById(billReceived.getId())).thenReturn(Optional.of(receivedInvoice));
		
		// when
		receivedInvoiceService.billReceived(TestData.accessToken, billReceived);

		// then
		verify(receivedInvoiceRepo, never()).save(any());
	}
	
	@Test
	public void givenNewInvoice_whenBillReceived_thenSaveInvoiceWithDownloadIdAndRawXml() throws IOException {
		// given
		final ReceivedMessage billReceived = new ReceivedMessage(3009239535L, LocalDateTime.now().minusDays(10),
				TestData.companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+TestData.companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
		
		final Path testInvoiceZipPath = Paths.get("src","test","resources", "3009239535.zip");
		final Path testInvoiceXmlPath = Paths.get("src","test","resources", "5006514680.xml");
		
		when(anafApi.downloadResponse(TestData.accessToken, billReceived.getId().toString()))
		.thenReturn(ResponseEntity.ok(Files.readAllBytes(testInvoiceZipPath)));
		
		// when
		receivedInvoiceService.billReceived(TestData.accessToken, billReceived);

		// then
		final ArgumentCaptor<ReceivedInvoice> saveCaptor = ArgumentCaptor.forClass(ReceivedInvoice.class);
		verify(receivedInvoiceRepo).save(saveCaptor.capture());
		final ReceivedInvoice capturedInvoice = saveCaptor.getValue();
		
		assertThat(capturedInvoice.getId()).isEqualTo(billReceived.getId());
		assertThat(capturedInvoice.getUploadIndex()).isEqualTo(billReceived.getUploadIndex());
		assertThat(capturedInvoice.getDownloadId()).isEqualTo(billReceived.getId().toString());
		assertThat(capturedInvoice.getXmlRaw()).isEqualTo(Files.readString(testInvoiceXmlPath));
		assertThat(capturedInvoice.getIssueDate()).isEqualTo(LocalDate.of(2024, 1, 25));
	}
	
	@Test
	public void givenDownloadZipInternalError_whenBillReceived_thenPropagateException() {
		// given
		final ReceivedMessage billReceived = new ReceivedMessage(3009239535L, LocalDateTime.now().minusDays(10),
				TestData.companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+TestData.companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
		
		when(anafApi.downloadResponse(TestData.accessToken, billReceived.getId().toString())).thenReturn(ResponseEntity.badRequest().build());
		
		// when
		// then
		final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> receivedInvoiceService.billReceived(TestData.accessToken, billReceived));
		assertThat(ex.getMessage()).isEqualTo(HttpStatus.BAD_REQUEST.toString());
	}
	
	@Test
	public void givenNewCreditNote_whenBillReceived_thenSaveCreditNoteWithDownloadIdAndRawXml() throws IOException {
		// given
		final ReceivedMessage billReceived = new ReceivedMessage(123L, LocalDateTime.now().minusDays(10),
				TestData.companyToken.getTaxId(), "4680", "Factura cu id_incarcare=4680 emisa de cif_emitent=RO7568475 pentru cif_beneficiar="+TestData.companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
		
		final Path testInvoiceZipPath = Paths.get("src","test","resources", "credit_note.zip");
		final Path testInvoiceXmlPath = Paths.get("src","test","resources", "credit_note.xml");
		
		when(anafApi.downloadResponse(TestData.accessToken, billReceived.getId().toString()))
		.thenReturn(ResponseEntity.ok(Files.readAllBytes(testInvoiceZipPath)));
		
		// when
		receivedInvoiceService.billReceived(TestData.accessToken, billReceived);

		// then
		final ArgumentCaptor<ReceivedCreditNote> saveCaptor = ArgumentCaptor.forClass(ReceivedCreditNote.class);
		verify(receivedCreditNoteRepo).save(saveCaptor.capture());
		final ReceivedCreditNote capturedCreditNote = saveCaptor.getValue();
		
		assertThat(capturedCreditNote.getId()).isEqualTo(billReceived.getId());
		assertThat(capturedCreditNote.getUploadIndex()).isEqualTo(billReceived.getUploadIndex());
		assertThat(capturedCreditNote.getDownloadId()).isEqualTo(billReceived.getId().toString());
		assertThat(capturedCreditNote.getXmlRaw()).isEqualTo(Files.readString(testInvoiceXmlPath));
		assertThat(capturedCreditNote.getIssueDate()).isEqualTo(LocalDate.of(2024, 1, 31));
	}
}
