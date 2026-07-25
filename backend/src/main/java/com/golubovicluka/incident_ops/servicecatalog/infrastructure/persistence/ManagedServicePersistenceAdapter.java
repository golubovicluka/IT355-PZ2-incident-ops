package com.golubovicluka.incident_ops.servicecatalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.infrastructure.persistence.TeamJpaEntity;
import com.golubovicluka.incident_ops.servicecatalog.domain.DuplicateManagedServiceNameException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceInUseException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class ManagedServicePersistenceAdapter implements ManagedServiceRepository {

	private final SpringDataManagedServiceRepository repository;
	private final EntityManager entityManager;
	private final ManagedServicePersistenceMapper mapper =
			new ManagedServicePersistenceMapper();

	public ManagedServicePersistenceAdapter(
			SpringDataManagedServiceRepository repository,
			EntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Override
	public ManagedService save(ManagedService service) {
		TeamJpaEntity team = entityManager.getReference(
				TeamJpaEntity.class,
				service.owningTeam().id());
		try {
			ManagedServiceJpaEntity saved = repository.saveAndFlush(
					mapper.toJpaEntity(service, team));
			return new ManagedService(
					saved.getId(),
					service.name(),
					service.description(),
					service.criticality(),
					service.owningTeam());
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateManagedServiceNameException(exception);
		}
	}

	@Override
	public List<ManagedService> findAll() {
		return repository.findAllByOrderByNameAsc().stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public Optional<ManagedService> findById(long id) {
		return repository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<ManagedService> findByName(String name) {
		return repository.findByName(name).map(mapper::toDomain);
	}

	@Override
	public void delete(ManagedService service) {
		try {
			repository.deleteById(service.id());
			repository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw new ManagedServiceInUseException(exception);
		}
	}
}
