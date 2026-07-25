package com.golubovicluka.incident_ops.escalation.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import com.golubovicluka.incident_ops.escalation.application.CreateEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.DeleteEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.ListEscalationPolicies;
import com.golubovicluka.incident_ops.escalation.application.PolicyManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.escalation.application.UpdateEscalationPolicy;
import com.golubovicluka.incident_ops.escalation.application.command.CreateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.command.UpdateEscalationPolicyCommand;
import com.golubovicluka.incident_ops.escalation.application.dto.EscalationPolicyView;
import com.golubovicluka.incident_ops.escalation.domain.DuplicateEscalationPolicyException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyInUseException;
import com.golubovicluka.incident_ops.escalation.domain.EscalationPolicyNotFoundException;
import com.golubovicluka.incident_ops.escalation.domain.InvalidEscalationPolicyDeadlineException;
import com.golubovicluka.incident_ops.incident.domain.IncidentPriority;
import com.golubovicluka.incident_ops.shared.config.ApplicationClockConfiguration;
import com.golubovicluka.incident_ops.shared.security.ApiAccessDeniedHandler;
import com.golubovicluka.incident_ops.shared.security.ApiAuthenticationEntryPoint;
import com.golubovicluka.incident_ops.shared.security.JwtConfiguration;
import com.golubovicluka.incident_ops.shared.security.SecurityConfiguration;
import com.golubovicluka.incident_ops.shared.web.ApiErrorWriter;
import com.golubovicluka.incident_ops.shared.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EscalationPolicyAdminController.class)
@Import({
		ApplicationClockConfiguration.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class,
		EscalationPolicyExceptionHandler.class
})
@ActiveProfiles("test")
class EscalationPolicyAdminControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListEscalationPolicies listPolicies;

	@MockitoBean
	private CreateEscalationPolicy createPolicy;

	@MockitoBean
	private UpdateEscalationPolicy updatePolicy;

	@MockitoBean
	private DeleteEscalationPolicy deletePolicy;

	@Test
	@WithMockUser(roles = "ADMIN")
	void administratorCreatesUpdatesListsAndDeletesPolicies() throws Exception {
		EscalationPolicyView created = policyView(IncidentPriority.SEV1, 10, 45);
		given(createPolicy.execute(new CreateEscalationPolicyCommand(
				7L,
				IncidentPriority.SEV1,
				Duration.ofMinutes(10),
				Duration.ofMinutes(45)))).willReturn(created);
		given(updatePolicy.execute(new UpdateEscalationPolicyCommand(
				42L,
				7L,
				IncidentPriority.SEV2,
				Duration.ofMinutes(20),
				Duration.ofMinutes(120))))
				.willReturn(policyView(IncidentPriority.SEV2, 20, 120));
		given(listPolicies.execute()).willReturn(List.of(created));

		mockMvc.perform(post("/api/admin/policies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(7, "SEV1", 10, 45)))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location",
						"http://localhost/api/admin/policies/42"))
				.andExpect(jsonPath("$.managedService.id").value(7))
				.andExpect(jsonPath("$.managedService.name").value("Payments API"))
				.andExpect(jsonPath("$.priority").value("SEV1"))
				.andExpect(jsonPath("$.acknowledgementMinutes").value(10))
				.andExpect(jsonPath("$.resolutionMinutes").value(45));
		mockMvc.perform(put("/api/admin/policies/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(7, "SEV2", 20, 120)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.priority").value("SEV2"))
				.andExpect(jsonPath("$.resolutionMinutes").value(120));
		mockMvc.perform(get("/api/admin/policies"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(42));
		mockMvc.perform(delete("/api/admin/policies/42"))
				.andExpect(status().isNoContent());

		verify(deletePolicy).execute(42L);
	}

	@Test
	void anonymousUserCannotReadPolicies() throws Exception {
		mockMvc.perform(get("/api/admin/policies"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "RESPONDER")
	void responderCannotManagePolicies() throws Exception {
		mockMvc.perform(get("/api/admin/policies"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/policies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(7, "SEV1", 10, 45)))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void invalidFieldsReturnStructuredErrors() throws Exception {
		mockMvc.perform(post("/api/admin/policies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(0, "SEV1", 0, -1)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.managedServiceId")
						.value("Managed service must be selected"))
				.andExpect(jsonPath("$.fieldErrors.acknowledgementMinutes")
						.value("Acknowledgement deadline must be positive"))
				.andExpect(jsonPath("$.fieldErrors.resolutionMinutes")
						.value("Resolution deadline must be positive"));

		mockMvc.perform(post("/api/admin/policies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(7, "SEV0", 10, 45)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Request body is missing or malformed"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void businessFailuresReturnStableClientResponses() throws Exception {
		CreateEscalationPolicyCommand command = new CreateEscalationPolicyCommand(
				7L,
				IncidentPriority.SEV1,
				Duration.ofMinutes(10),
				Duration.ofMinutes(45));
		given(createPolicy.execute(command))
				.willThrow(new DuplicateEscalationPolicyException())
				.willThrow(new PolicyManagedServiceNotFoundException())
				.willThrow(new InvalidEscalationPolicyDeadlineException(
						"acknowledgementMinutes",
						"Acknowledgement deadline must not exceed the resolution deadline"));
		willThrow(new EscalationPolicyNotFoundException())
				.given(deletePolicy).execute(404L);
		willThrow(new EscalationPolicyInUseException(
				new IllegalStateException("referenced")))
				.given(deletePolicy).execute(42L);

		mockMvc.perform(post("/api/admin/policies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(7, "SEV1", 10, 45)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"An escalation policy already exists for this service and priority"));
		mockMvc.perform(post("/api/admin/policies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(7, "SEV1", 10, 45)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.managedServiceId")
						.value("Managed service does not exist"));
		mockMvc.perform(post("/api/admin/policies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(policyRequest(7, "SEV1", 10, 45)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.acknowledgementMinutes").value(
						"Acknowledgement deadline must not exceed the resolution deadline"));
		mockMvc.perform(delete("/api/admin/policies/404"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message")
						.value("Escalation policy was not found"));
		mockMvc.perform(delete("/api/admin/policies/42"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Escalation policy cannot be deleted while active incidents reference it"));
	}

	private EscalationPolicyView policyView(
			IncidentPriority priority,
			long acknowledgementMinutes,
			long resolutionMinutes) {
		return new EscalationPolicyView(
				42L,
				new EscalationPolicyView.ManagedServiceView(7L, "Payments API"),
				priority,
				acknowledgementMinutes,
				resolutionMinutes);
	}

	private String policyRequest(
			long managedServiceId,
			String priority,
			long acknowledgementMinutes,
			long resolutionMinutes) {
		return """
				{
				  "managedServiceId": %d,
				  "priority": "%s",
				  "acknowledgementMinutes": %d,
				  "resolutionMinutes": %d
				}
				""".formatted(
				managedServiceId,
				priority,
				acknowledgementMinutes,
				resolutionMinutes);
	}
}
