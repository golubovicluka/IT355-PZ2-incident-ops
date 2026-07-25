package com.golubovicluka.incident_ops.servicecatalog.web;

import java.util.List;

import com.golubovicluka.incident_ops.servicecatalog.application.ListManagedServices;
import com.golubovicluka.incident_ops.servicecatalog.web.response.ManagedServiceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogs/services")
public class ManagedServiceCatalogController {

	private final ListManagedServices listManagedServices;

	public ManagedServiceCatalogController(ListManagedServices listManagedServices) {
		this.listManagedServices = listManagedServices;
	}

	@GetMapping
	List<ManagedServiceResponse> list() {
		return listManagedServices.execute().stream()
				.map(ManagedServiceResponse::from)
				.toList();
	}
}
