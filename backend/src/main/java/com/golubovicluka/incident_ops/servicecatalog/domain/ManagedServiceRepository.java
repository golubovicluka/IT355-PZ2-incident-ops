package com.golubovicluka.incident_ops.servicecatalog.domain;

import java.util.List;
import java.util.Optional;

public interface ManagedServiceRepository {

	ManagedService save(ManagedService service);

	List<ManagedService> findAll();

	Optional<ManagedService> findById(long id);

	Optional<ManagedService> findByName(String name);

	void delete(ManagedService service);
}
