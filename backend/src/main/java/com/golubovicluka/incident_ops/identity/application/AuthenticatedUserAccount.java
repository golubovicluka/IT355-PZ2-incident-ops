package com.golubovicluka.incident_ops.identity.application;

import java.util.Set;

import com.golubovicluka.incident_ops.identity.domain.Role;

public record AuthenticatedUserAccount(
		String username,
		String displayName,
		Set<Role> roles) {

	public AuthenticatedUserAccount {
		roles = Set.copyOf(roles);
	}
}
