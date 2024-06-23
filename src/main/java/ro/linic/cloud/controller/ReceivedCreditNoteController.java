package ro.linic.cloud.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.linic.cloud.entity.ReceivedCreditNote;
import ro.linic.cloud.repository.ReceivedCreditNoteRepository;

@RestController
@RequestMapping("/creditNotes")
public class ReceivedCreditNoteController {
	@Autowired private ReceivedCreditNoteRepository receivedCreditNoteRepo;

	@GetMapping
    public ResponseEntity<Iterable<ReceivedCreditNote>> findAll() {
		return ResponseEntity.ok(receivedCreditNoteRepo.findAll());
    }
	
	@GetMapping("/search/between")
    public ResponseEntity<List<ReceivedCreditNote>> findAllByIssueDateBetween(final LocalDate start, final LocalDate end) {
		return ResponseEntity.ok(receivedCreditNoteRepo.findAllByIssueDateBetween(start, end));
    }
}
