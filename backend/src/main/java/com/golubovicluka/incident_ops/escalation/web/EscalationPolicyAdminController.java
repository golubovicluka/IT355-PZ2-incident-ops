package com.golubovicluka.incident_ops.escalation.web;

import java.time.Duration;
import java.util.List;

import com.golubovicluka.incident_ops.escalation.application.CreateEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.DeleteEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.ListEscalationPolicies;
import com.golubovicluka.incident_ops.escalation.application.UpdateEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.command.CreateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.command.UpdateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.escalation.web.request.EscalationPolicyRequest;
import com.golubovicluka.incident_ops.escalation.web.response.EscalationPolicyResponse;
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
@RequestMapping("/api/admin/policies")
public class EscalationPolicyAdminController {

	private final ListEscalationPolicies listPolicies;
	private final CreateEscalationPolicy createPolicy;
	private final UpdateEscalationPolicy updatePolicy;
	private final DeleteEscalationPolicy deletePolicy;

	public EscalationPolicyAdminController(
			ListEscalationPolicies listPolicies,
			CreateEscalationPolicy createPolicy,
			UpdateEscalationPolicy updatePolicy,
			DeleteEscalationPolicy deletePolicy) {
		this.listPolicies = listPolicies;
		this.createPolicy = createPolicy;
		this.updatePolicy = updatePolicy;
		this.deletePolicy = deletePolicy;
	}

	@GetMapping
	List<EscalationPolicyResponse> list() {
		return listPolicies.execute().stream()
				.map(EscalationPolicyResponse::from)
				.toList();
	}

	@PostMapping
	ResponseEntity<EscalationPolicyResponse> create(
			@Valid @RequestBody EscalationPolicyRequest request) {
		EscalationPolicyView created = createPolicy.execute(
				new CreateEscalationPolicyCommand(
						request.managedServiceId(),
						request.priority(),
						Duration.ofMinutes(request.acknowledgementMinutes()),
						Duration.ofMinutes(request.resolutionMinutes())));
		return ResponseEntity
				.created(ServletUriComponentsBuilder.fromCurrentRequest()
						.path("/{id}")
						.buildAndExpand(created.id())
						.toUri())
				.body(EscalationPolicyResponse.from(created));
	}

	@PutMapping("/{id}")
	EscalationPolicyResponse update(
			@PathVariable long id,
			@Valid @RequestBody EscalationPolicyRequest request) {
		return EscalationPolicyResponse.from(updatePolicy.execute(
				new UpdateEscalationPolicyCommand(
						id,
						request.managedServiceId(),
						request.priority(),
						Duration.ofMinutes(request.acknowledgementMinutes()),
						Duration.ofMinutes(request.resolutionMinutes()))));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable long id) {
		deletePolicy.execute(id);
		return ResponseEntity.noContent().build();
	}
}
