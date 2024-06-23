package ro.linic.cloud.pojo.anaf;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "header", namespace = AnafUploadResponseHeader.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class AnafUploadResponseHeader {
	public static final String NAMESPACE = "mfp:anaf:dgti:spv:respUploadFisier:v1";
	private static final String STATUS_OK = "0";
	
	@XmlAttribute(name = "ExecutionStatus")
	private String executionStatus;
	@XmlAttribute(name = "index_incarcare", required = false)
	private String uploadIndex;
	@XmlElement(name = "Errors", namespace = NAMESPACE)
    private List<AnafResponseError> errors = new ArrayList<>();
	
	public boolean isExecutionStatusOk() {
		return STATUS_OK.equalsIgnoreCase(executionStatus);
	}
}
