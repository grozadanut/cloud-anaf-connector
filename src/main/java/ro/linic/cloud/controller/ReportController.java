package ro.linic.cloud.controller;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.linic.cloud.entity.ReportedInvoice;
import ro.linic.cloud.pojo.Invoice;
import ro.linic.cloud.repository.ReportedInvoiceRepository;
import ro.linic.cloud.service.ReportService;

@RestController
@RequestMapping("/report")
public class ReportController {
	@Autowired private ReportService reportService;
	@Autowired private ReportedInvoiceRepository reportedInvoiceRepo;
	
	@PutMapping
    public ResponseEntity<ReportedInvoice> reportInvoice(@RequestParam final int companyId, @RequestBody final Invoice invoice) {
		return ResponseEntity.ok(reportService.reportInvoice(companyId, invoice));
    }
	
	@GetMapping("/test")
    public ResponseEntity<String> testAnafApi(@RequestParam final int companyId, @RequestParam(required = false) final String name) {
		return reportService.testOauth(companyId, name);
    }
	
	@GetMapping("/{id}")
    public ResponseEntity<ReportedInvoice> findById(@PathVariable(name = "id") final long invoiceId) {
		return ResponseEntity.ok(reportedInvoiceRepo.findById(invoiceId).orElse(null));
    }
	
	@PostMapping
    public ResponseEntity<Iterable<ReportedInvoice>> findAll() {
		return ResponseEntity.ok(reportedInvoiceRepo.findAll());
    }
	
	@GetMapping("/search/findAllById")
    public ResponseEntity<Iterable<ReportedInvoice>> findAllById(@RequestParam final Collection<Long> ids) {
		return ResponseEntity.ok(reportedInvoiceRepo.findAllById(ids));
    }
	
	@PostMapping("/check")
	public ResponseEntity<String> checkReportedInvoicesState(@RequestParam final int companyId) {
		reportService.checkReportedInvoicesState(companyId);
		return ResponseEntity.ok("OK");
    }
	
	@PostMapping("/download")
	public ResponseEntity<byte[]> downloadResponse(@RequestParam final int companyId, @RequestBody final String downloadId) {
		return reportService.downloadResponse(companyId, downloadId);
    }
}
