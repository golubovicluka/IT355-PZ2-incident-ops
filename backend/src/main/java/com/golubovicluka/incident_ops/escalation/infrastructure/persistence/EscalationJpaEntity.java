package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import java.time.Instant;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.UserAccountJpaEntity;
import com.golubovicluka.incident_ops.incident.domain.IncidentEvent;
import com.golubovicluka.incident_ops.incident.infrastructure.persistence.IncidentJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
		name = "escalations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_escalations_incident_level",
				columnNames = {"incident_id", "level"}))
class EscalationJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "incident_id", nullable = false, updatable = false)
	private IncidentJpaEntity incident;

	@Column(nullable = false, updatable = false)
	private int level;

	@Column(
			nullable = false,
			updatable = false,
			length = IncidentEvent.MAX_ESCALATION_REASON_LENGTH)
	private String reason;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false, updatable = false)
	private UserAccountJpaEntity actor;

	@Column(name = "escalated_at", nullable = false, updatable = false)
	private Instant escalatedAt;

	protected EscalationJpaEntity() {
	}

	EscalationJpaEntity(
			IncidentJpaEntity incident,
			int level,
			String reason,
			UserAccountJpaEntity actor,
			Instant escalatedAt) {
		this.incident = incident;
		this.level = level;
		this.reason = reason;
		this.actor = actor;
		this.escalatedAt = escalatedAt;
	}

	Long getId() {
		return id;
	}

	IncidentJpaEntity getIncident() {
		return incident;
	}

	int getLevel() {
		return level;
	}

	String getReason() {
		return reason;
	}

	UserAccountJpaEntity getActor() {
		return actor;
	}

	Instant getEscalatedAt() {
		return escalatedAt;
	}
}
