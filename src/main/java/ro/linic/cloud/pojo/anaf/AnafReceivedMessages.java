package ro.linic.cloud.pojo.anaf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import lombok.Data;

@Data
public class AnafReceivedMessages {
	public static final String NO_MESSAGES_ERROR = "No messages found";
	public static final String TOO_MANY_MESSAGES_ERROR = "Too many messages found. Use pagination!";
	
	@JsonProperty("mesaje")
	private List<AnafReceivedMessage> messages = new ArrayList<>();
	@JsonDeserialize(using = ErrorDeserializer.class)
	@JsonProperty("eroare")
	private String error;
	
	private static class ErrorDeserializer extends StringDeserializer {
		private static final long serialVersionUID = 1L;
		
		@Override
		public String deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
			final String value = super.deserialize(p, ctxt);
			
			if (StringUtils.startsWithIgnoreCase(value, "Nu exista mesaje in ultimele"))
				return NO_MESSAGES_ERROR;
			else if (StringUtils.startsWithIgnoreCase(value, "Lista de mesaje este mai mare decat numarul de"))
				return TOO_MANY_MESSAGES_ERROR;
			
			return value;
		}
	}
}
