package com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.TeamJpaEntity;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import jakarta.persistence.Column;
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
		name = "managed_services",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_managed_services_name",
				columnNames = "name"))
class ManagedServiceJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(nullable = false, length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Criticality criticality;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owning_team_id", nullable = false)
	private TeamJpaEntity owningTeam;

	protected ManagedServiceJpaEntity() {
	}

	ManagedServiceJpaEntity(
			Long id,
			String name,
			String description,
			Criticality criticality,
			TeamJpaEntity owningTeam) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.criticality = criticality;
		this.owningTeam = owningTeam;
	}

	Long getId() {
		return id;
	}

	String getName() {
		return name;
	}

	String getDescription() {
		return description;
	}

	Criticality getCriticality() {
		return criticality;
	}

	TeamJpaEntity getOwningTeam() {
		return owningTeam;
	}
}
