package ro.linic.cloud.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ReportedInvoice {
	@Id
	private Long invoiceId;
	
	@Enumerated(EnumType.STRING)
	private ReportState state;
	private String uploadIndex;
	private String downloadId;
	private String errorMessage;
	
	public enum ReportState {
		UPLOAD_ERROR, WAITING_VALIDATION, REJECTED_INVALID, SENT;
	}
}
