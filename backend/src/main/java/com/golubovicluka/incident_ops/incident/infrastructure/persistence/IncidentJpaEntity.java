package com.golubovicluka.incident_ops.incident.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.UserAccountJpaEntity;
import com.golubovicluka.incident_ops.incident.domain.Incident;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence.ManagedServiceJpaEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "incidents",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_incidents_reference_code",
				columnNames = "reference_code"))
public class IncidentJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(
			name = "reference_code",
			nullable = false,
			unique = true,
			updatable = false,
			length = Incident.MAX_REFERENCE_CODE_LENGTH)
	private String referenceCode;

	@Column(nullable = false, length = Incident.MAX_TITLE_LENGTH)
	private String title;

	@Column(nullable = false, length = Incident.MAX_DESCRIPTION_LENGTH)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private IncidentPriority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private IncidentStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "managed_service_id", nullable = false)
	private ManagedServiceJpaEntity managedService;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_id", nullable = false, updatable = false)
	private UserAccountJpaEntity reporter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private UserAccountJpaEntity assignee;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(
			mappedBy = "incident",
			cascade = CascadeType.PERSIST,
			orphanRemoval = false)
	@OrderBy("occurredAt ASC, id ASC")
	private List<IncidentEventJpaEntity> events = new ArrayList<>();

	protected IncidentJpaEntity() {
	}

	IncidentJpaEntity(
			String referenceCode,
			String title,
			String description,
			IncidentPriority priority,
			IncidentStatus status,
			ManagedServiceJpaEntity managedService,
			UserAccountJpaEntity reporter,
			UserAccountJpaEntity assignee,
			Instant createdAt,
			Instant updatedAt) {
		this.referenceCode = referenceCode;
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.status = status;
		this.managedService = managedService;
		this.reporter = reporter;
		this.assignee = assignee;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	void addEvent(IncidentEventJpaEntity event) {
		events.add(event);
	}

	void updateEditableFields(
			String title,
			String description,
			IncidentPriority priority,
			ManagedServiceJpaEntity managedService,
			UserAccountJpaEntity assignee,
			Instant updatedAt) {
		this.title = title;
		this.description = description;
		this.priority = priority;
		this.managedService = managedService;
		this.assignee = assignee;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	String getReferenceCode() {
		return referenceCode;
	}

	String getTitle() {
		return title;
	}

	String getDescription() {
		return description;
	}

	IncidentPriority getPriority() {
		return priority;
	}

	IncidentStatus getStatus() {
		return status;
	}

	ManagedServiceJpaEntity getManagedService() {
		return managedService;
	}

	UserAccountJpaEntity getReporter() {
		return reporter;
	}

	UserAccountJpaEntity getAssignee() {
		return assignee;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	List<IncidentEventJpaEntity> getEvents() {
		return List.copyOf(events);
	}
}
