package ro.linic.cloud.pojo.anaf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class AnafResponseError {
	@XmlAttribute(name = "errorMessage")
	private String message;
}
