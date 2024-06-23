package ro.linic.cloud.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.entity.ReportedInvoice;
import ro.linic.cloud.pojo.Invoice;

public interface ReportService {
	ReportedInvoice reportInvoice(int companyId, Invoice invoice);
	ResponseEntity<String> testOauth(int companyId, String name);
	void checkReportedInvoicesState(int companyId);
	ResponseEntity<byte[]> downloadResponse(int companyId, String downloadId);
	ResponseEntity<List<ReceivedMessage>> checkForReceivedMessages(int companyId, final int days);
}
