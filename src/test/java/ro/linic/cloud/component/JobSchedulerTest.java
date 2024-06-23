package ro.linic.cloud.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ro.linic.cloud.TestData;
import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;
import ro.linic.cloud.repository.CompanyOAuthTokenRepository;
import ro.linic.cloud.repository.ReceivedMessageRepository;
import ro.linic.cloud.service.ReportService;

@ExtendWith(MockitoExtension.class)
public class JobSchedulerTest {
	@Mock private ReportService reportService;
	@Mock private ReceivedMessageRepository receivedMessageRepo;
	@Mock private CompanyOAuthTokenRepository companyOAuthTokenRepos;
	@InjectMocks private JobScheduler jobScheduler;
	
	@BeforeEach
	public void setup() throws Exception {
		TestData.init();
	}
	
	@Test
	public void givenNoMessagesSaved_whenCheckReceivedMessages_thenSearchLast60Days() {
		// given
		when(receivedMessageRepo.findFirstByOrderByCreationDateDesc()).thenReturn(null);
		when(companyOAuthTokenRepos.findAll()).thenReturn(List.of(TestData.companyToken));
		
		// when
		jobScheduler.scheduleCheckReceivedMessages();
		
		// then
		final ArgumentCaptor<Integer> companyIdCaptor = ArgumentCaptor.forClass(Integer.class);
		final ArgumentCaptor<Integer> daysCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(reportService).checkForReceivedMessages(companyIdCaptor.capture(), daysCaptor.capture());
		final Integer capturedCompanyId = companyIdCaptor.getValue();
		final Integer capturedDays = daysCaptor.getValue();
		
		assertThat(capturedCompanyId).isEqualTo(TestData.companyToken.getCompanyId());
		assertThat(capturedDays).isEqualTo(60);
	}
	
	@Test
	public void givenLastMessage10DaysAgo_whenCheckReceivedMessages_thenSearchLast11Days() {
		// given
		final ReceivedMessage billReceived = new ReceivedMessage(3009239535L, LocalDateTime.now().minusDays(10),
				TestData.companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+TestData.companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
		
		when(receivedMessageRepo.findFirstByOrderByCreationDateDesc()).thenReturn(billReceived);
		when(companyOAuthTokenRepos.findAll()).thenReturn(List.of(TestData.companyToken));
		
		// when
		jobScheduler.scheduleCheckReceivedMessages();
		
		// then
		final ArgumentCaptor<Integer> companyIdCaptor = ArgumentCaptor.forClass(Integer.class);
		final ArgumentCaptor<Integer> daysCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(reportService).checkForReceivedMessages(companyIdCaptor.capture(), daysCaptor.capture());
		final Integer capturedCompanyId = companyIdCaptor.getValue();
		final Integer capturedDays = daysCaptor.getValue();
		
		assertThat(capturedCompanyId).isEqualTo(TestData.companyToken.getCompanyId());
		assertThat(capturedDays).isEqualTo(11);
	}
	
	@Test
	public void givenLastMessage100DaysAgo_whenCheckReceivedMessages_thenSearchLast60Days() {
		// given
		final ReceivedMessage billReceived = new ReceivedMessage(3009239535L, LocalDateTime.now().minusDays(100),
				TestData.companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+TestData.companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
		
		when(receivedMessageRepo.findFirstByOrderByCreationDateDesc()).thenReturn(billReceived);
		when(companyOAuthTokenRepos.findAll()).thenReturn(List.of(TestData.companyToken));
		
		// when
		jobScheduler.scheduleCheckReceivedMessages();
		
		// then
		final ArgumentCaptor<Integer> companyIdCaptor = ArgumentCaptor.forClass(Integer.class);
		final ArgumentCaptor<Integer> daysCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(reportService).checkForReceivedMessages(companyIdCaptor.capture(), daysCaptor.capture());
		final Integer capturedCompanyId = companyIdCaptor.getValue();
		final Integer capturedDays = daysCaptor.getValue();
		
		assertThat(capturedCompanyId).isEqualTo(TestData.companyToken.getCompanyId());
		assertThat(capturedDays).isEqualTo(60);
	}
	
	@Test
	public void givenLastMessage10DaysAhead_whenCheckReceivedMessages_thenSearchLast1Day() {
		// given
		final ReceivedMessage billReceived = new ReceivedMessage(3009239535L, LocalDateTime.now().plusDays(10),
				TestData.companyToken.getTaxId(), "5006514680", "Factura cu id_incarcare=5006514680 emisa de cif_emitent=1485236 pentru cif_beneficiar="+TestData.companyToken.getTaxId(),
    			AnafReceivedMessageType.BILL_RECEIVED);
		
		when(receivedMessageRepo.findFirstByOrderByCreationDateDesc()).thenReturn(billReceived);
		when(companyOAuthTokenRepos.findAll()).thenReturn(List.of(TestData.companyToken));
		
		// when
		jobScheduler.scheduleCheckReceivedMessages();
		
		// then
		final ArgumentCaptor<Integer> companyIdCaptor = ArgumentCaptor.forClass(Integer.class);
		final ArgumentCaptor<Integer> daysCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(reportService).checkForReceivedMessages(companyIdCaptor.capture(), daysCaptor.capture());
		final Integer capturedCompanyId = companyIdCaptor.getValue();
		final Integer capturedDays = daysCaptor.getValue();
		
		assertThat(capturedCompanyId).isEqualTo(TestData.companyToken.getCompanyId());
		assertThat(capturedDays).isEqualTo(1);
	}
}
