package com.golubovicluka.incident_ops.incident.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

interface SpringDataIncidentRepository
		extends JpaRepository<IncidentJpaEntity, Long>,
		JpaSpecificationExecutor<IncidentJpaEntity> {

	@Override
	@EntityGraph(attributePaths = {"managedService", "reporter", "assignee"})
	List<IncidentJpaEntity> findAll(
			Specification<IncidentJpaEntity> specification,
			Sort sort);

	@Override
	@EntityGraph(attributePaths = {
			"managedService",
			"reporter",
			"assignee",
			"events",
			"events.actor"
	})
	Optional<IncidentJpaEntity> findById(Long id);
}
