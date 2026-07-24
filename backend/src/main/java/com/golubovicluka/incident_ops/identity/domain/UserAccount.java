package com.golubovicluka.incident_ops.identity.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class UserAccount {

	private final Long id;
	private final String username;
	private final String displayName;
	private final String passwordHash;
	private final Set<Role> roles;
	private final Team team;

	public UserAccount(
			Long id,
			String username,
			String displayName,
			String passwordHash,
			Set<Role> roles,
			Team team) {
		this.id = id;
		this.username = requireText(username, "username").toLowerCase(Locale.ROOT);
		this.displayName = requireText(displayName, "displayName");
		this.passwordHash = requireText(passwordHash, "passwordHash");
		this.roles = immutableRoles(roles);
		this.team = Objects.requireNonNull(team, "team must not be null");
	}

	public static UserAccount create(
			String username,
			String displayName,
			String passwordHash,
			Set<Role> roles,
			Team team) {
		return new UserAccount(null, username, displayName, passwordHash, roles, team);
	}

	public Long id() {
		return id;
	}

	public String username() {
		return username;
	}

	public String displayName() {
		return displayName;
	}

	public String passwordHash() {
		return passwordHash;
	}

	public Set<Role> roles() {
		return roles;
	}

	public Team team() {
		return team;
	}

	@Override
	public String toString() {
		return "UserAccount[id=%s, username=%s, displayName=%s, roles=%s, team=%s]"
				.formatted(id, username, displayName, roles, team);
	}

	private static Set<Role> immutableRoles(Set<Role> roles) {
		Objects.requireNonNull(roles, "roles must not be null");
		if (roles.isEmpty()) {
			throw new IllegalArgumentException("roles must not be empty");
		}
		if (roles.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("roles must not contain null");
		}
		return Collections.unmodifiableSet(EnumSet.copyOf(roles));
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return normalized;
	}
}
