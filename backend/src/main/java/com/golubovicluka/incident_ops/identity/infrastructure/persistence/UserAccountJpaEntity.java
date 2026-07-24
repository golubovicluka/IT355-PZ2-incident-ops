package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "user_accounts",
		uniqueConstraints = @UniqueConstraint(name = "uk_user_accounts_username", columnNames = "username"))
class UserAccountJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String username;

	@Column(name = "display_name", nullable = false, length = 150)
	private String displayName;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
			name = "user_account_roles",
			joinColumns = @JoinColumn(name = "user_account_id", nullable = false))
	@Column(name = "role", nullable = false, length = 32)
	@Enumerated(EnumType.STRING)
	private Set<UserRoleJpa> roles = EnumSet.noneOf(UserRoleJpa.class);

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "team_id", nullable = false)
	private TeamJpaEntity team;

	protected UserAccountJpaEntity() {
	}

	UserAccountJpaEntity(
			Long id,
			String username,
			String displayName,
			String passwordHash,
			Set<UserRoleJpa> roles,
			TeamJpaEntity team) {
		this.id = id;
		this.username = username;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
		this.roles = EnumSet.copyOf(roles);
		this.team = team;
	}

	Long getId() {
		return id;
	}

	String getUsername() {
		return username;
	}

	String getDisplayName() {
		return displayName;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	Set<UserRoleJpa> getRoles() {
		return roles;
	}

	TeamJpaEntity getTeam() {
		return team;
	}
}
