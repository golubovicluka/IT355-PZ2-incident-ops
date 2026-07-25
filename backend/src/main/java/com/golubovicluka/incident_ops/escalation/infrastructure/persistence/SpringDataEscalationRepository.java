package com.golubovicluka.incident_ops.escalation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataEscalationRepository
		extends JpaRepository<EscalationJpaEntity, Long> {

	@Query("""
			select coalesce(max(escalation.level), 0)
			from EscalationJpaEntity escalation
			where escalation.incident.id = :incidentId
			""")
	int findHighestLevelByIncidentId(@Param("incidentId") long incidentId);
}
