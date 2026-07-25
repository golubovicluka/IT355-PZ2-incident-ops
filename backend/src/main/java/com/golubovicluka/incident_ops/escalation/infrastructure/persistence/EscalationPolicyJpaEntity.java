package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence.ManagedServiceJpaEntity;
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
		name = "escalation_policies",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_escalation_policies_service_priority",
				columnNames = {"managed_service_id", "priority"}))
class EscalationPolicyJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "managed_service_id", nullable = false)
	private ManagedServiceJpaEntity managedService;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private IncidentPriority priority;

	@Column(name = "acknowledgement_minutes", nullable = false)
	private long acknowledgementMinutes;

	@Column(name = "resolution_minutes", nullable = false)
	private long resolutionMinutes;

	protected EscalationPolicyJpaEntity() {
	}

	EscalationPolicyJpaEntity(
			Long id,
			ManagedServiceJpaEntity managedService,
			IncidentPriority priority,
			long acknowledgementMinutes,
			long resolutionMinutes) {
		this.id = id;
		this.managedService = managedService;
		this.priority = priority;
		this.acknowledgementMinutes = acknowledgementMinutes;
		this.resolutionMinutes = resolutionMinutes;
	}

	Long getId() {
		return id;
	}

	ManagedServiceJpaEntity getManagedService() {
		return managedService;
	}

	IncidentPriority getPriority() {
		return priority;
	}

	long getAcknowledgementMinutes() {
		return acknowledgementMinutes;
	}

	long getResolutionMinutes() {
		return resolutionMinutes;
	}
}
