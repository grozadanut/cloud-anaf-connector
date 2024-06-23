package ro.linic.cloud.pojo.anaf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "header", namespace = AnafUploadStateResponseHeader.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class AnafUploadStateResponseHeader {
	public static final String NAMESPACE = "mfp:anaf:dgti:efactura:stareMesajFactura:v1";
	public static final String OK_STATE = "ok";
	public static final String NOK_STATE = "nok";
	public static final String PENDING_STATE = "in prelucrare";
	
	@XmlAttribute(name = "stare", required = false)
	private String state;
	@XmlAttribute(name = "id_descarcare", required = false)
	private String downloadId;
	@XmlElement(name = "Errors", namespace = NAMESPACE)
    private List<AnafResponseError> errors = new ArrayList<>();
	
	public boolean isStateOk() {
		return OK_STATE.equalsIgnoreCase(state);
	}
	
	public boolean isStateNok() {
		return NOK_STATE.equalsIgnoreCase(state);
	}
	
	public boolean isStatePending() {
		return PENDING_STATE.equalsIgnoreCase(state);
	}
	
	public String prettyErrorMessage() {
		if (isStateOk())
			return "";
		
		if (!errors.isEmpty())
			return errors.stream()
					.map(AnafResponseError::getMessage)
					.collect(Collectors.joining(System.lineSeparator()));
		
		return state;
	}
}
