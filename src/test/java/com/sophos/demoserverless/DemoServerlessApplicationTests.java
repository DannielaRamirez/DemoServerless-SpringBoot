package com.sophos.demoserverless;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophos.demoserverless.beans.EmpleadoRequest;
import com.sophos.demoserverless.beans.EmpleadoResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class DemoServerlessApplicationTests {

	private final MockMvc mvc;
	private final ObjectMapper objectMapper;

	@Autowired
	public DemoServerlessApplicationTests(MockMvc mvc) {
		this.mvc = mvc;

		objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
	}

	@Test
	void testApi() throws Exception {
		// Payload
		final EmpleadoRequest request = new EmpleadoRequest();
		request.setCedula("5647382910");
		request.setNombre("Empleado Prueba");
		request.setEdad(30);
		request.setCiudad("Cartagena");

		// Crea un empleado
		final EmpleadoResponse responsePost = objectMapper.readValue(
			mvc.perform(
				post("/empleados/")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.codigo").exists())
			.andDo(print())
			.andReturn()
				.getResponse().getContentAsString()
			, EmpleadoResponse.class
		);

		// Valida la respuesta
		Assertions.assertThat(responsePost)
			.hasFieldOrPropertyWithValue("cedula", request.getCedula())
			.hasFieldOrPropertyWithValue("nombre", request.getNombre())
			.hasFieldOrPropertyWithValue("edad", request.getEdad())
			.hasFieldOrPropertyWithValue("ciudad", request.getCiudad())
		;

		// Elimina el empleado creado
		mvc.perform(delete("/empleados/{codigo}", responsePost.getCodigo()))
			.andExpect(status().isOk())
		;
	}

}
