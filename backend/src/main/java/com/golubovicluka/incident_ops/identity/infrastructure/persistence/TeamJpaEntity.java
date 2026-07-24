package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "teams",
		uniqueConstraints = @UniqueConstraint(name = "uk_teams_name", columnNames = "name"))
class TeamJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	protected TeamJpaEntity() {
	}

	TeamJpaEntity(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	Long getId() {
		return id;
	}

	String getName() {
		return name;
	}
}
