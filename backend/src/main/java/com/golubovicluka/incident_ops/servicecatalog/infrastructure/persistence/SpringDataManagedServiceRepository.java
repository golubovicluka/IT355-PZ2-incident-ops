package com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataManagedServiceRepository
		extends JpaRepository<ManagedServiceJpaEntity, Long> {

	@EntityGraph(attributePaths = "owningTeam")
	List<ManagedServiceJpaEntity> findAllByOrderByNameAsc();

	@Override
	@EntityGraph(attributePaths = "owningTeam")
	Optional<ManagedServiceJpaEntity> findById(Long id);

	@EntityGraph(attributePaths = "owningTeam")
	Optional<ManagedServiceJpaEntity> findByName(String name);
}
