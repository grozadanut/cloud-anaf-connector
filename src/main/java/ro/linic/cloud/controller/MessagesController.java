package ro.linic.cloud.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;
import ro.linic.cloud.repository.ReceivedMessageRepository;
import ro.linic.cloud.service.ReportService;

@RestController
@RequestMapping("/messages")
public class MessagesController {
	@Autowired private ReportService reportService;
	@Autowired private ReceivedMessageRepository receivedMessageRepo;
	
	@GetMapping
    public ResponseEntity<Iterable<ReceivedMessage>> receivedMessages() {
		return ResponseEntity.ok(receivedMessageRepo.findAll());
    }
	
	@GetMapping("/search/between")
    public ResponseEntity<List<ReceivedMessage>> receivedMessagesBetween(final LocalDateTime start, final LocalDateTime end) {
		return ResponseEntity.ok(receivedMessageRepo.findAllByCreationDateBetweenAndMessageTypeIn(start, end,
				List.of(AnafReceivedMessageType.BILL_SENT, AnafReceivedMessageType.BILL_RECEIVED)));
    }
	
	@PostMapping("/check")
    public ResponseEntity<List<ReceivedMessage>> manualCheckForReceivedMessages(@RequestParam final int companyId,
    		@RequestParam final int days) {
		return reportService.checkForReceivedMessages(companyId, days);
    }
}
