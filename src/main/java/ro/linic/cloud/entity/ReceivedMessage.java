package ro.linic.cloud.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;

@Entity
@Data @AllArgsConstructor @NoArgsConstructor
public class ReceivedMessage {
	@Id private Long id;
	private LocalDateTime creationDate;
	private String taxId;
	private String uploadIndex;
	@Column(columnDefinition = "TEXT")
	private String details;
	@Enumerated(EnumType.STRING)
	private AnafReceivedMessageType messageType;
}
