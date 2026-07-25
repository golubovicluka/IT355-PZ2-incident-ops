package com.golubovicluka.incident_ops.incident.domain;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository {

	Incident save(Incident incident);

	List<Incident> findAll(IncidentCriteria criteria);

	Optional<Incident> findById(long id);
}
