package ro.linic.cloud.component;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.util.MultiValueMap;

public class AnafRequestEntityConverter implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {

	private final OAuth2AuthorizationCodeGrantRequestEntityConverter defaultConverter;

	public AnafRequestEntityConverter() {
        defaultConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();
    }

	@Override
	public RequestEntity<?> convert(final OAuth2AuthorizationCodeGrantRequest req) {
		final RequestEntity<?> entity = defaultConverter.convert(req);
		final MultiValueMap<String, String> params = (MultiValueMap<String, String>) entity.getBody();
		params.add("token_content_type", "jwt");
		return new RequestEntity<>(params, entity.getHeaders(), entity.getMethod(), entity.getUrl());
	}

}