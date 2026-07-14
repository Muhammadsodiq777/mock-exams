package com.mockexams.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
class MinioConfig {

	@Bean
	MinioClient minioClient(BackendEnvironmentProperties properties) {
		BackendEnvironmentProperties.Minio minio = properties.minio();
		return MinioClient.builder()
				.endpoint(minio.endpoint())
				.credentials(minio.accessKey(), minio.secretKey())
				.build();
	}
}
