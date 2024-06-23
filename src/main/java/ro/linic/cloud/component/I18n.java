package ro.linic.cloud.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class I18n {
	@Autowired private MessageSource messageSource;

	public String getMessage(final String code) {
		// Attention LocaleContextHolder.getLocale() is thread based,
		// maybe you need some fallback locale
		return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
	}
}