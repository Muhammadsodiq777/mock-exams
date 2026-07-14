package com.mockexams.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.unit.DataSize;

class MultipartUploadPropertiesTest {

	@Test
	void multipartParserLimitAllowsLargestBusinessUploadRule() throws IOException {
		MockEnvironment environment = new MockEnvironment();
		environment.setConversionService(new ApplicationConversionService());
		environment.getPropertySources()
				.addFirst(new PropertiesPropertySource("applicationProperties", applicationProperties()));

		MultipartProperties properties = Binder.get(environment)
				.bind("spring.servlet.multipart", MultipartProperties.class)
				.orElseThrow(() -> new AssertionError("Multipart properties should be configured"));

		assertThat(properties.getMaxFileSize()).isGreaterThan(DataSize.ofMegabytes(100));
		assertThat(properties.getMaxRequestSize()).isGreaterThan(properties.getMaxFileSize());
	}

	private Properties applicationProperties() throws IOException {
		Properties properties = new Properties();
		try (var input = new ClassPathResource("application.properties").getInputStream()) {
			properties.load(input);
		}
		return properties;
	}
}
