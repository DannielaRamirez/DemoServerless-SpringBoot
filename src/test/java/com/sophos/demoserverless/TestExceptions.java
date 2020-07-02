package com.sophos.demoserverless;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophos.demoserverless.beans.EmpleadoRequest;
import com.sophos.demoserverless.model.Empleado;
import com.sophos.demoserverless.repository.EmpleadoRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
public class TestExceptions {

	private final MockMvc mvc;
	private final ObjectMapper objectMapper;

	@Autowired
	public TestExceptions(MockMvc mvc) {
		this.mvc = mvc;

		objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
	}

	@MockBean
	private EmpleadoRepository empleadoRepository;

	@Test
	void testExceptions() throws Exception {
		// GetAll vacío
		BDDMockito.given(empleadoRepository.findAll()).willReturn(List.of());
		final Exception exGetAll = mvc.perform(get("/empleados"))
			.andExpect(status().isNotFound())
			.andReturn().getResolvedException()
		;
		Assertions.assertThat(exGetAll).isInstanceOf(ResponseStatusException.class);

		// Empleado dummy inexistente
		final UUID codigo = UUID.randomUUID();

		// Get vacío
		BDDMockito.given(empleadoRepository.findById(codigo)).willReturn(Optional.empty());
		final Exception exGet = mvc.perform(get("/empleados/{codigo}", codigo))
			.andExpect(status().isNotFound())
			.andReturn().getResolvedException()
		;
		Assertions.assertThat(exGet).isInstanceOf(ResponseStatusException.class);

		// Search vacío
		BDDMockito.given(empleadoRepository.search(codigo.toString())).willReturn(List.of());
		final Exception exSearch = mvc.perform(get("/empleados/buscar/{query}", codigo.toString()))
			.andExpect(status().isNotFound())
			.andReturn().getResolvedException()
		;
		Assertions.assertThat(exSearch).isInstanceOf(ResponseStatusException.class);

		// Cédula prueba
		final String cedula = UUID.randomUUID().toString();

		// Payload
		final EmpleadoRequest request = new EmpleadoRequest();
		request.setCedula(cedula);
		request.setNombre("Empleado Prueba");
		request.setEdad(30);
		request.setCiudad("Ciudad de prueba");

		// Empleado dummy
		final Empleado empleado = new Empleado();
		empleado.setSk(codigo.toString());
		empleado.setCedula(cedula);

		// Actualiza empleado inexistente
		final Exception exPutId = mvc.perform(
			put("/empleados/{codigo}", codigo)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isNotFound())
			.andReturn().getResolvedException()
		;
		Assertions.assertThat(exPutId).isInstanceOf(ResponseStatusException.class);

		// Post con cédula existente
		BDDMockito.given(empleadoRepository.findByCedula(cedula)).willReturn(List.of(empleado));
		final Exception exPost = mvc.perform(
				post("/empleados")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isConflict())
			.andReturn().getResolvedException()
		;
		Assertions.assertThat(exPost).isInstanceOf(ResponseStatusException.class);

		// Actualiza cédula en uso
		empleado.setSk(UUID.randomUUID().toString());
		BDDMockito.given(empleadoRepository.findById(codigo)).willReturn(Optional.of(empleado));
		final Exception exPutCed = mvc.perform(
			put("/empleados/{codigo}", codigo)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isConflict())
			.andReturn().getResolvedException()
		;
		Assertions.assertThat(exPutCed).isInstanceOf(ResponseStatusException.class);

	}

}
