package ro.linic.cloud.pojo.anaf;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class AnafReceivedMessage {
	private String id;
	@JsonProperty("data_creare")
	@JsonDeserialize(using = AnafDateDeserializer.class)
	private LocalDateTime creationDate;
	@JsonProperty("cif")
	private String taxId;
	@JsonProperty("id_solicitare")
	private String uploadIndex;
	@JsonProperty("detalii")
	private String details;
	@JsonProperty("tip")
	private AnafReceivedMessageType messageType;
	
	public enum AnafReceivedMessageType {
		@JsonProperty("FACTURA PRIMITA")
		BILL_RECEIVED,
		@JsonProperty("FACTURA TRIMISA")
		BILL_SENT,
		@JsonProperty("ERORI FACTURA")
		BILL_ERRORS;
	}
}
