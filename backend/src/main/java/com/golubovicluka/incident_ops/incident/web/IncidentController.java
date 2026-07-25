package com.golubovicluka.incident_ops.incident.web;

import java.util.List;

import com.golubovicluka.incident_ops.incident.application.AddIncidentNote;
import com.golubovicluka.incident_ops.incident.application.ChangeIncidentStatus;
import com.golubovicluka.incident_ops.incident.application.CreateIncident;
import com.golubovicluka.incident_ops.incident.application.GetIncident;
import com.golubovicluka.incident_ops.incident.application.ListIncidents;
import com.golubovicluka.incident_ops.incident.application.UpdateIncident;
import com.golubovicluka.incident_ops.incident.application.command.AddIncidentNoteCommand;
import com.golubovicluka.incident_ops.incident.application.command.ChangeIncidentStatusCommand;
import com.golubovicluka.incident_ops.incident.application.command.CreateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.command.UpdateIncidentCommand;
import com.golubovicluka.incident_ops.incident.application.dto.IncidentDetailView;
import com.golubovicluka.incident_ops.incident.domain.IncidentCriteria;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.incident.domain.IncidentStatus;
import com.golubovicluka.incident_ops.incident.web.request.IncidentRequest;
import com.golubovicluka.incident_ops.incident.web.request.IncidentNoteRequest;
import com.golubovicluka.incident_ops.incident.web.request.IncidentStatusRequest;
import com.golubovicluka.incident_ops.incident.web.response.IncidentDetailResponse;
import com.golubovicluka.incident_ops.incident.web.response.IncidentSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

	private final ListIncidents listIncidents;
	private final GetIncident getIncident;
	private final CreateIncident createIncident;
	private final UpdateIncident updateIncident;
	private final ChangeIncidentStatus changeIncidentStatus;
	private final AddIncidentNote addIncidentNote;

	public IncidentController(
			ListIncidents listIncidents,
			GetIncident getIncident,
			CreateIncident createIncident,
			UpdateIncident updateIncident,
			ChangeIncidentStatus changeIncidentStatus,
			AddIncidentNote addIncidentNote) {
		this.listIncidents = listIncidents;
		this.getIncident = getIncident;
		this.createIncident = createIncident;
		this.updateIncident = updateIncident;
		this.changeIncidentStatus = changeIncidentStatus;
		this.addIncidentNote = addIncidentNote;
	}

	@GetMapping
	List<IncidentSummaryResponse> list(
			@RequestParam(required = false) IncidentStatus status,
			@RequestParam(required = false) IncidentPriority priority,
			@RequestParam(required = false) Long serviceId) {
		if (serviceId != null && serviceId <= 0) {
			throw new InvalidIncidentFilterException(
					"serviceId",
					"Managed service must be selected");
		}
		return listIncidents.execute(
						new IncidentCriteria(status, priority, serviceId))
				.stream()
				.map(IncidentSummaryResponse::from)
				.toList();
	}

	@GetMapping("/{id}")
	IncidentDetailResponse detail(@PathVariable long id) {
		return IncidentDetailResponse.from(getIncident.execute(id));
	}

	@PostMapping
	ResponseEntity<IncidentDetailResponse> create(
			@Valid @RequestBody IncidentRequest request,
			Authentication authentication) {
		IncidentDetailView created = createIncident.execute(
				new CreateIncidentCommand(
						request.title(),
						request.description(),
						request.priority(),
						request.managedServiceId(),
						request.assigneeId(),
						authentication.getName()));
		return ResponseEntity
				.created(ServletUriComponentsBuilder.fromCurrentRequest()
						.path("/{id}")
						.buildAndExpand(created.id())
						.toUri())
				.body(IncidentDetailResponse.from(created));
	}

	@PutMapping("/{id}")
	IncidentDetailResponse update(
			@PathVariable long id,
			@Valid @RequestBody IncidentRequest request) {
		return IncidentDetailResponse.from(updateIncident.execute(
				new UpdateIncidentCommand(
						id,
						request.title(),
						request.description(),
						request.priority(),
						request.managedServiceId(),
						request.assigneeId())));
	}

	@PutMapping("/{id}/status")
	IncidentDetailResponse changeStatus(
			@PathVariable long id,
			@Valid @RequestBody IncidentStatusRequest request,
			Authentication authentication) {
		return IncidentDetailResponse.from(changeIncidentStatus.execute(
				new ChangeIncidentStatusCommand(
						id,
						request.status(),
						authentication.getName())));
	}

	@PostMapping("/{id}/events")
	IncidentDetailResponse addNote(
			@PathVariable long id,
			@Valid @RequestBody IncidentNoteRequest request,
			Authentication authentication) {
		return IncidentDetailResponse.from(addIncidentNote.execute(
				new AddIncidentNoteCommand(
						id,
						request.note(),
						authentication.getName())));
	}
}
