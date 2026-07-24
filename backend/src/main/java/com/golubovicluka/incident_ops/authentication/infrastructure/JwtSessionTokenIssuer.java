package com.golubovicluka.incident_ops.authentication.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.golubovicluka.incident_ops.authentication.application.AuthenticatedSession;
import com.golubovicluka.incident_ops.authentication.application.SessionTokenIssuer;
import com.golubovicluka.incident_ops.identity.application.AuthenticatedUserAccount;
import com.golubovicluka.incident_ops.shared.security.SecurityProperties;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class JwtSessionTokenIssuer implements SessionTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final SecurityProperties securityProperties;
	private final Clock clock;

	JwtSessionTokenIssuer(
			JwtEncoder jwtEncoder,
			SecurityProperties securityProperties,
			Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.securityProperties = securityProperties;
		this.clock = clock;
	}

	@Override
	public AuthenticatedSession issue(AuthenticatedUserAccount userAccount) {
		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plus(securityProperties.tokenLifetime());
		List<String> roles = userAccount.roles().stream()
				.map(Enum::name)
				.sorted()
				.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(userAccount.username())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("displayName", userAccount.displayName())
				.claim("roles", roles)
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
		Set<String> responseRoles = roles.stream().collect(Collectors.toUnmodifiableSet());
		return new AuthenticatedSession(
				token,
				expiresAt,
				userAccount.username(),
				userAccount.displayName(),
				responseRoles);
	}
}
