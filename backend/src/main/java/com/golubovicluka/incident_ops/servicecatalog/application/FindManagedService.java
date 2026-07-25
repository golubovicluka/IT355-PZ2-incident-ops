package com.golubovicluka.incident_ops.servicecatalog.application;

import java.util.Optional;

import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindManagedService {

	private final ManagedServiceRepository services;

	public FindManagedService(ManagedServiceRepository services) {
		this.services = services;
	}

	@Transactional(readOnly = true)
	public Optional<ManagedServiceView> execute(long id) {
		return services.findById(id).map(ManagedServiceView::from);
	}
}
