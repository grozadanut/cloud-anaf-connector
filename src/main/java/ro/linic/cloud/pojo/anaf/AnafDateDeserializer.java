package ro.linic.cloud.pojo.anaf;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class AnafDateDeserializer extends StdDeserializer<LocalDateTime> {
	private static final long serialVersionUID = 1L;
	
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

	public AnafDateDeserializer() {
		this(null);
	}

	public AnafDateDeserializer(final Class<?> vc) {
		super(vc);
	}

	@Override
	public LocalDateTime deserialize(final JsonParser jsonparser, final DeserializationContext context)
			throws IOException, JsonProcessingException {
		final String date = jsonparser.getText();
		try {
			return LocalDateTime.parse(date, formatter);
		} catch (final DateTimeParseException e) {
			throw new RuntimeException(e);
		}
	}
}
