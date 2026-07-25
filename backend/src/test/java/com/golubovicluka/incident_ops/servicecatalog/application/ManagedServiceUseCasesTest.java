package com.golubovicluka.incident_ops.servicecatalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.golubovicluka.incident_ops.identity.application.FindTeam;
import com.golubovicluka.incident_ops.identity.application.dto.TeamView;
import com.golubovicluka.incident_ops.servicecatalog.application.command.CreateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.command.UpdateManagedServiceCommand;
import com.golubovicluka.incident_ops.servicecatalog.application.dto.ManagedServiceView;
import com.golubovicluka.incident_ops.servicecatalog.domain.Criticality;
import com.golubovicluka.incident_ops.servicecatalog.domain.DuplicateManagedServiceNameException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedService;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceInUseException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceNotFoundException;
import com.golubovicluka.incident_ops.servicecatalog.domain.ManagedServiceRepository;
import com.golubovicluka.incident_ops.servicecatalog.domain.OwningTeam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagedServiceUseCasesTest {

	@Mock
	private ManagedServiceRepository services;

	@Mock
	private FindTeam findTeam;

	private CreateManagedService createManagedService;
	private ListManagedServices listManagedServices;
	private UpdateManagedService updateManagedService;
	private DeleteManagedService deleteManagedService;

	@BeforeEach
	void setUp() {
		createManagedService = new CreateManagedService(services, findTeam);
		listManagedServices = new ListManagedServices(services);
		updateManagedService = new UpdateManagedService(services, findTeam);
		deleteManagedService = new DeleteManagedService(services);
	}

	@Test
	void createsServiceForAnExistingOwningTeam() {
		when(findTeam.execute(7L))
				.thenReturn(Optional.of(new TeamView(7L, "Platform Operations")));
		when(services.findByName("Payments API")).thenReturn(Optional.empty());
		when(services.save(any(ManagedService.class))).thenReturn(new ManagedService(
				42L,
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				new OwningTeam(7L, "Platform Operations")));

		ManagedServiceView created = createManagedService.execute(
				new CreateManagedServiceCommand(
						"  Payments API  ",
						"  Processes card payments.  ",
						Criticality.CRITICAL,
						7L));

		assertThat(created).isEqualTo(new ManagedServiceView(
				42L,
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				new ManagedServiceView.TeamView(7L, "Platform Operations")));
	}

	@Test
	void rejectsCreationWhenTheOwningTeamDoesNotExist() {
		when(findTeam.execute(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> createManagedService.execute(
				new CreateManagedServiceCommand(
						"Payments API",
						"Processes card payments.",
						Criticality.CRITICAL,
						404L)))
				.isInstanceOf(OwningTeamNotFoundException.class)
				.hasMessage("Owning team does not exist");

		verify(services, never()).save(any(ManagedService.class));
	}

	@Test
	void rejectsCreationWhenTheNormalizedNameAlreadyExists() {
		when(findTeam.execute(7L))
				.thenReturn(Optional.of(new TeamView(7L, "Platform Operations")));
		when(services.findByName("Payments API")).thenReturn(Optional.of(service()));

		assertThatThrownBy(() -> createManagedService.execute(
				new CreateManagedServiceCommand(
						" Payments API ",
						"Processes card payments.",
						Criticality.CRITICAL,
						7L)))
				.isInstanceOf(DuplicateManagedServiceNameException.class);

		verify(services, never()).save(any(ManagedService.class));
	}

	@Test
	void listsServicesInRepositoryOrder() {
		when(services.findAll()).thenReturn(List.of(service()));

		assertThat(listManagedServices.execute()).containsExactly(view());
	}

	@Test
	void updatesServiceAndOwningTeam() {
		ManagedService existing = service();
		when(services.findById(42L)).thenReturn(Optional.of(existing));
		when(findTeam.execute(8L))
				.thenReturn(Optional.of(new TeamView(8L, "Incident Response")));
		when(services.findByName("Checkout API")).thenReturn(Optional.empty());
		when(services.save(any(ManagedService.class))).thenAnswer(invocation ->
				invocation.getArgument(0));

		ManagedServiceView updated = updateManagedService.execute(
				new UpdateManagedServiceCommand(
						42L,
						"Checkout API",
						"Coordinates checkout.",
						Criticality.HIGH,
						8L));

		assertThat(updated).isEqualTo(new ManagedServiceView(
				42L,
				"Checkout API",
				"Coordinates checkout.",
				Criticality.HIGH,
				new ManagedServiceView.TeamView(8L, "Incident Response")));
	}

	@Test
	void reportsMissingServiceDuringUpdateAndDelete() {
		when(services.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> updateManagedService.execute(
				new UpdateManagedServiceCommand(
						404L,
						"Missing",
						"Missing service.",
						Criticality.LOW,
						7L)))
				.isInstanceOf(ManagedServiceNotFoundException.class);
		assertThatThrownBy(() -> deleteManagedService.execute(404L))
				.isInstanceOf(ManagedServiceNotFoundException.class);
	}

	@Test
	void deletesAnExistingService() {
		ManagedService existing = service();
		when(services.findById(42L)).thenReturn(Optional.of(existing));

		deleteManagedService.execute(42L);

		verify(services).delete(existing);
	}

	@Test
	void preservesReferenceConflictFromPersistence() {
		ManagedService existing = service();
		when(services.findById(42L)).thenReturn(Optional.of(existing));
		org.mockito.Mockito.doThrow(new ManagedServiceInUseException())
				.when(services).delete(existing);

		assertThatThrownBy(() -> deleteManagedService.execute(42L))
				.isInstanceOf(ManagedServiceInUseException.class)
				.hasMessage("Managed service cannot be deleted while other records reference it");
	}

	private ManagedService service() {
		return new ManagedService(
				42L,
				"Payments API",
				"Processes card payments.",
				Criticality.CRITICAL,
				new OwningTeam(7L, "Platform Operations"));
	}

	private ManagedServiceView view() {
		return ManagedServiceView.from(service());
	}
}
