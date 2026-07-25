package com.golubovicluka.incident_ops.incident.infrastructure.persistence;

import java.time.Instant;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.UserAccountJpaEntity;
import com.golubovicluka.incident_ops.incident.domain.IncidentEventKind;
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

@Entity
@Table(name = "incident_events")
class IncidentEventJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incident_id", nullable = false, updatable = false)
	private IncidentJpaEntity incident;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 32)
	private IncidentEventKind kind;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false, updatable = false)
	private UserAccountJpaEntity actor;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	protected IncidentEventJpaEntity() {
	}

	IncidentEventJpaEntity(
			IncidentJpaEntity incident,
			IncidentEventKind kind,
			UserAccountJpaEntity actor,
			Instant occurredAt) {
		this.incident = incident;
		this.kind = kind;
		this.actor = actor;
		this.occurredAt = occurredAt;
	}

	Long getId() {
		return id;
	}

	IncidentEventKind getKind() {
		return kind;
	}

	UserAccountJpaEntity getActor() {
		return actor;
	}

	Instant getOccurredAt() {
		return occurredAt;
	}
}
