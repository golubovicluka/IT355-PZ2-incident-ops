package com.golubovicluka.incident_ops.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccountJpaEntity, Long> {

	@EntityGraph(attributePaths = {"team", "roles"})
	List<UserAccountJpaEntity> findAllByOrderByDisplayNameAsc();

	@EntityGraph(attributePaths = {"team", "roles"})
	Optional<UserAccountJpaEntity> findByUsername(String username);

	boolean existsByTeam_Id(Long teamId);
}
