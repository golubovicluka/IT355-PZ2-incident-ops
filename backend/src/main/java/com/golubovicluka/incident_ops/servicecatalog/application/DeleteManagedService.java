package com.golubovicluka.incident_ops.servicecatalog.application;

import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteManagedService {

	private final ManagedServiceRepository services;

	public DeleteManagedService(ManagedServiceRepository services) {
		this.services = services;
	}

	@Transactional
	public void execute(long id) {
		ManagedService service = services.findById(id)
				.orElseThrow(ManagedServiceNotFoundException::new);
		services.delete(service);
	}
}
