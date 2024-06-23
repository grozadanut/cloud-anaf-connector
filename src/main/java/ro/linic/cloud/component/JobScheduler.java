package ro.linic.cloud.component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ro.linic.cloud.entity.CompanyOAuthToken;
import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.repository.CompanyOAuthTokenRepository;
import ro.linic.cloud.repository.ReceivedMessageRepository;
import ro.linic.cloud.service.ReportService;

@Component
public class JobScheduler {
	@Autowired private ReportService reportService;
	@Autowired private ReceivedMessageRepository receivedMessageRepo;
	@Autowired private CompanyOAuthTokenRepository companyOAuthTokenRepos;
	
	@Scheduled(cron = "${cron.reported-invoice.check-state:-}")
    public void checkReportedInvoicesState() {
		companyOAuthTokenRepos.findAll().forEach(token -> reportService.checkReportedInvoicesState(token.getCompanyId()));
    }
	
	@Scheduled(cron = "${cron.received-message.find-new:-}")
    public void scheduleCheckReceivedMessages() {
		companyOAuthTokenRepos.findAll().forEach(this::checkReceivedMessages);
    }
	
	private void checkReceivedMessages(final CompanyOAuthToken token) {
		final ReceivedMessage latestMessage = receivedMessageRepo.findFirstByOrderByCreationDateDesc();
		long days = 60;
		
		if (latestMessage != null)
		{
			final long elapsedDays = ChronoUnit.DAYS.between(latestMessage.getCreationDate(), LocalDateTime.now());
			days = Math.max(Math.min(elapsedDays+1, 60), 1);
		}
		
		reportService.checkForReceivedMessages(token.getCompanyId(), (int) days);
	}
}
