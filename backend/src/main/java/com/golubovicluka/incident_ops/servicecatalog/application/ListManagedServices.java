package com.golubovicluka.incident_ops.servicecatalog.application;

import java.util.List;

import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListManagedServices {

	private final ManagedServiceRepository services;

	public ListManagedServices(ManagedServiceRepository services) {
		this.services = services;
	}

	@Transactional(readOnly = true)
	public List<ManagedServiceView> execute() {
		return services.findAll().stream().map(ManagedServiceView::from).toList();
	}
}
