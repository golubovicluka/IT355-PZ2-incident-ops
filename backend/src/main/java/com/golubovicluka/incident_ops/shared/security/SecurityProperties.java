package com.golubovicluka.incident_ops.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("incident-ops.security")
public final class SecurityProperties {

	private static final int MINIMUM_HS256_KEY_BYTES = 32;

	private final SecretKey signingKey;
	private final Duration tokenLifetime;
	private final List<String> corsAllowedOrigins;

	public SecurityProperties(
			String jwtSecret,
			Duration tokenLifetime,
			List<String> corsAllowedOrigins) {
		byte[] secretBytes = requireText(jwtSecret, "JWT signing secret")
				.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MINIMUM_HS256_KEY_BYTES) {
			throw new IllegalArgumentException(
					"JWT signing secret must contain at least " + MINIMUM_HS256_KEY_BYTES + " UTF-8 bytes");
		}
		this.signingKey = new SecretKeySpec(secretBytes, "HmacSHA256");
		this.tokenLifetime = requirePositive(tokenLifetime);
		this.corsAllowedOrigins = requireOrigins(corsAllowedOrigins);
	}

	public SecretKey signingKey() {
		return signingKey;
	}

	public Duration tokenLifetime() {
		return tokenLifetime;
	}

	public List<String> corsAllowedOrigins() {
		return corsAllowedOrigins;
	}

	@Override
	public String toString() {
		return "SecurityProperties[signingKey=[REDACTED], tokenLifetime=%s, corsAllowedOrigins=%s]"
				.formatted(tokenLifetime, corsAllowedOrigins);
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " must be configured");
		}
		return value;
	}

	private static Duration requirePositive(Duration value) {
		Objects.requireNonNull(value, "JWT token lifetime must be configured");
		if (value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException("JWT token lifetime must be positive");
		}
		return value;
	}

	private static List<String> requireOrigins(List<String> origins) {
		Objects.requireNonNull(origins, "CORS allowed origins must be configured");
		List<String> normalized = origins.stream()
				.map(String::strip)
				.filter(origin -> !origin.isEmpty())
				.distinct()
				.toList();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("At least one CORS allowed origin must be configured");
		}
		return normalized;
	}
}
