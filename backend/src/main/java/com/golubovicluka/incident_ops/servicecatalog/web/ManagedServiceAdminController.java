package com.golubovicluka.incident_ops.servicecatalog.web;

import java.util.List;

import com.golubovicluka.incident_ops.servicecatalog.application.CreateManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.DeleteManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.ListManagedServices;
import com.golubovicluka.incident_ops.servicecatalog.application.UpdateManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.command.CreateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.command.UpdateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.web.request.ManagedServiceRequest;
import com.golubovicluka.incident_ops.servicecatalog.web.response.ManagedServiceResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/services")
public class ManagedServiceAdminController {

	private final ListManagedServices listManagedServices;
	private final CreateManagedService createManagedService;
	private final UpdateManagedService updateManagedService;
	private final DeleteManagedService deleteManagedService;

	public ManagedServiceAdminController(
			ListManagedServices listManagedServices,
			CreateManagedService createManagedService,
			UpdateManagedService updateManagedService,
			DeleteManagedService deleteManagedService) {
		this.listManagedServices = listManagedServices;
		this.createManagedService = createManagedService;
		this.updateManagedService = updateManagedService;
		this.deleteManagedService = deleteManagedService;
	}

	@GetMapping
	List<ManagedServiceResponse> list() {
		return listManagedServices.execute().stream()
				.map(ManagedServiceResponse::from)
				.toList();
	}

	@PostMapping
	ResponseEntity<ManagedServiceResponse> create(
			@Valid @RequestBody ManagedServiceRequest request) {
		ManagedServiceView created = createManagedService.execute(
				new CreateManagedServiceCommand(
						request.name(),
						request.description(),
						request.criticality(),
						request.owningTeamId()));
		return ResponseEntity
				.created(ServletUriComponentsBuilder.fromCurrentRequest()
						.path("/{id}")
						.buildAndExpand(created.id())
						.toUri())
				.body(ManagedServiceResponse.from(created));
	}

	@PutMapping("/{id}")
	ManagedServiceResponse update(
			@PathVariable long id,
			@Valid @RequestBody ManagedServiceRequest request) {
		return ManagedServiceResponse.from(updateManagedService.execute(
				new UpdateManagedServiceCommand(
						id,
						request.name(),
						request.description(),
						request.criticality(),
						request.owningTeamId())));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable long id) {
		deleteManagedService.execute(id);
		return ResponseEntity.noContent().build();
	}
}
