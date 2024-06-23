package ro.linic.cloud.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data @AllArgsConstructor @NoArgsConstructor
public class ReceivedInvoice {
	@Id private Long id;
	private String uploadIndex;
	private String downloadId;
	@Column(columnDefinition = "TEXT")
	private String xmlRaw;
	private LocalDate issueDate;
	@Column(unique=true)
	private Long invoiceId;
}
