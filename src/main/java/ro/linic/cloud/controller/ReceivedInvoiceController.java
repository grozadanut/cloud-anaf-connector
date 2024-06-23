package ro.linic.cloud.controller;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.linic.cloud.entity.ReceivedInvoice;
import ro.linic.cloud.repository.ReceivedInvoiceRepository;

@RestController
@RequestMapping("/invoices")
public class ReceivedInvoiceController {
	@Autowired private ReceivedInvoiceRepository receivedInvoiceRepo;

	@GetMapping
    public ResponseEntity<Iterable<ReceivedInvoice>> receivedInvoices() {
		return ResponseEntity.ok(receivedInvoiceRepo.findAll());
    }
	
	@GetMapping("/search/between")
    public ResponseEntity<List<ReceivedInvoice>> receivedInvoicesBetween(final LocalDate start, final LocalDate end) {
		return ResponseEntity.ok(receivedInvoiceRepo.findAllByIssueDateBetween(start, end));
    }
	
	@GetMapping("/search/byInvoiceIdIn")
    public ResponseEntity<List<ReceivedInvoice>> findByInvoiceIdIn(@RequestParam final Collection<Long> invoiceIds) {
		return ResponseEntity.ok(receivedInvoiceRepo.findByInvoiceIdIn(invoiceIds));
    }
	
	@PatchMapping("/{id}")
    public ResponseEntity<ReceivedInvoice> updateInvoiceId(@PathVariable(name = "id") final long id,
    		@RequestBody(required = false) final Long invoiceId) {
		return ResponseEntity.ok(receivedInvoiceRepo.findById(id)
				.map(inv -> inv.setInvoiceId(invoiceId))
				.map(receivedInvoiceRepo::save)
				.orElse(null));
    }
}
