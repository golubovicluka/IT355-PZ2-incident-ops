package com.golubovicluka.incident_ops.servicecatalog.web;

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

import java.util.List;

import com.golubovicluka.incident_ops.identity.application.ListAssignableUsers;
import com.golubovicluka.incident_ops.identity.application.dto.AssignableUserView;
import com.golubovicluka.incident_ops.identity.web.UserCatalogController;
import com.golubovicluka.incident_ops.servicecatalog.application.CreateManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.DeleteManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.ListManagedServices;
import com.golubovicluka.incident_ops.servicecatalog.application.OwningTeamNotFoundException;
import com.golubovicluka.incident_ops.servicecatalog.application.UpdateManagedService;
import com.golubovicluka.incident_ops.servicecatalog.application.command.CreateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.command.UpdateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.DuplicateManagedServiceNameException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceInUseException;
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

@WebMvcTest({
		ManagedServiceAdminController.class,
		ManagedServiceCatalogController.class,
		UserCatalogController.class
})
@Import({
		ApplicationClockConfiguration.class,
		ApiAccessDeniedHandler.class,
		ApiAuthenticationEntryPoint.class,
		ApiErrorWriter.class,
		ApiExceptionHandler.class,
		JwtConfiguration.class,
		SecurityConfiguration.class,
		ManagedServiceExceptionHandler.class
})
@ActiveProfiles("test")
class ManagedServiceControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListManagedServices listManagedServices;

	@MockitoBean
	private CreateManagedService createManagedService;

	@MockitoBean
	private UpdateManagedService updateManagedService;

	@MockitoBean
	private DeleteManagedService deleteManagedService;

	@MockitoBean
	private ListAssignableUsers listAssignableUsers;

	@Test
	@WithMockUser(roles = "ADMIN")
	void administratorCreatesUpdatesListsAndDeletesServices() throws Exception {
		ManagedServiceView created = serviceView("Payments API");
		given(createManagedService.execute(new CreateManagedServiceCommand(
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				7L))).willReturn(created);
		given(updateManagedService.execute(new UpdateManagedServiceCommand(
				42L,
				"Checkout API",
				"Coordinates checkout.",
				Criticality.HIGH,
				7L))).willReturn(serviceView("Checkout API"));
		given(listManagedServices.execute()).willReturn(List.of(created));

		mockMvc.perform(post("/api/admin/services")
						.contentType(MediaType.APPLICATION_JSON)
						.content(serviceRequest(
								"Payments API",
								"Processes card payments.",
								"CRITICAL",
								"7")))
				.andExpect(status().isCreated())
				.andExpect(header().string(
						"Location",
						"http://localhost/api/admin/services/42"))
				.andExpect(jsonPath("$.name").value("Payments API"))
				.andExpect(jsonPath("$.criticality").value("CRITICAL"))
				.andExpect(jsonPath("$.owningTeam.id").value(7));
		mockMvc.perform(put("/api/admin/services/42")
						.contentType(MediaType.APPLICATION_JSON)
						.content(serviceRequest(
								"Checkout API",
								"Coordinates checkout.",
								"HIGH",
								"7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Checkout API"));
		mockMvc.perform(get("/api/admin/services"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(42));
		mockMvc.perform(delete("/api/admin/services/42"))
				.andExpect(status().isNoContent());

		verify(deleteManagedService).execute(42L);
	}

	@Test
	@WithMockUser(roles = "RESPONDER")
	void responderReadsServiceAndAssignmentCatalogs() throws Exception {
		given(listManagedServices.execute()).willReturn(List.of(serviceView("Payments API")));
		given(listAssignableUsers.execute()).willReturn(List.of(
				new AssignableUserView(
						11L,
						"ana",
						"Ana Anić",
						new AssignableUserView.TeamView(7L, "Incident Response"))));

		mockMvc.perform(get("/api/catalogs/services"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Payments API"))
				.andExpect(jsonPath("$[0].owningTeam.name")
						.value("Platform Operations"));
		mockMvc.perform(get("/api/catalogs/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(11))
				.andExpect(jsonPath("$[0].displayName").value("Ana Anić"))
				.andExpect(jsonPath("$[0].team.name").value("Incident Response"))
				.andExpect(jsonPath("$[0].password").doesNotExist());
	}

	@Test
	void anonymousUserCannotReadCatalogs() throws Exception {
		mockMvc.perform(get("/api/catalogs/services"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/catalogs/users"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "RESPONDER")
	void responderCannotMutateServices() throws Exception {
		mockMvc.perform(get("/api/admin/services"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void invalidFieldsReturnStructuredErrors() throws Exception {
		mockMvc.perform(post("/api/admin/services")
						.contentType(MediaType.APPLICATION_JSON)
						.content(serviceRequest(" ", " ", "HIGH", "0")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name")
						.value("Managed service name is required"))
				.andExpect(jsonPath("$.fieldErrors.description")
						.value("Managed service description is required"))
				.andExpect(jsonPath("$.fieldErrors.owningTeamId")
						.value("Owning team must be selected"));

		mockMvc.perform(post("/api/admin/services")
						.contentType(MediaType.APPLICATION_JSON)
						.content(serviceRequest(
								"Payments API",
								"Processes card payments.",
								"URGENT",
								"7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("Request body is missing or malformed"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void domainFailuresReturnStableClientResponses() throws Exception {
		given(createManagedService.execute(new CreateManagedServiceCommand(
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				7L))).willThrow(new DuplicateManagedServiceNameException());
		given(createManagedService.execute(new CreateManagedServiceCommand(
				"Checkout API",
				"Coordinates checkout.",
				Criticality.HIGH,
				404L))).willThrow(new OwningTeamNotFoundException());
		willThrow(new ManagedServiceInUseException())
				.given(deleteManagedService).execute(42L);

		mockMvc.perform(post("/api/admin/services")
						.contentType(MediaType.APPLICATION_JSON)
						.content(serviceRequest(
								"Payments API",
								"Processes card payments.",
								"CRITICAL",
								"7")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("A managed service with this name already exists"));
		mockMvc.perform(post("/api/admin/services")
						.contentType(MediaType.APPLICATION_JSON)
						.content(serviceRequest(
								"Checkout API",
								"Coordinates checkout.",
								"HIGH",
								"404")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.owningTeamId")
						.value("Owning team does not exist"));
		mockMvc.perform(delete("/api/admin/services/42"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Managed service cannot be deleted while other records reference it"));
	}

	private ManagedServiceView serviceView(String name) {
		return new ManagedServiceView(
				42L,
				name,
				"Processes card payments.",
				Criticality.CRITICAL,
				new ManagedServiceView.TeamView(7L, "Platform Operations"));
	}

	private String serviceRequest(
			String name,
			String description,
			String criticality,
			String owningTeamId) {
		return """
				{
				  "name": "%s",
				  "description": "%s",
				  "criticality": "%s",
				  "owningTeamId": %s
				}
				""".formatted(name, description, criticality, owningTeamId);
	}
}
