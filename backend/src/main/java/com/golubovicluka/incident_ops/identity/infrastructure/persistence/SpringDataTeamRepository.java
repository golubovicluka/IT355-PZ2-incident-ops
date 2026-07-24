package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTeamRepository extends JpaRepository<TeamJpaEntity, Long> {

	Optional<TeamJpaEntity> findByName(String name);
}
