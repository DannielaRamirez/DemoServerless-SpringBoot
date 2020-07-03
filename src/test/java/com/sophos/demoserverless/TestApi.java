package com.sophos.demoserverless;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophos.demoserverless.beans.EmpleadoRequest;
import com.sophos.demoserverless.beans.EmpleadoResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class TestApi {

	private final MockMvc mvc;
	private final ObjectMapper objectMapper;

	@MockBean
	private AmazonSQS sqs;

	@Autowired
	public TestApi(MockMvc mvc) {
		this.mvc = mvc;

		objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
	}

	@Test
	void testApi() throws Exception {
		// Impide el registro en la cola de los eventos de prueba
		Mockito.doAnswer(invocation -> new SendMessageResult()).when(sqs).sendMessage(new SendMessageRequest());

		// Ping
		mvc.perform(get("/ping"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ping").exists())
		;

		// Instancia del código
		UUID codigo = null;

		// Payload
		final EmpleadoRequest request = new EmpleadoRequest();
		request.setCedula("5647382910");
		request.setNombre("Empleado Prueba");
		request.setEdad(30);
		request.setCiudad("Cartagena");

		try {

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

			// Almacena el código
			codigo = responsePost.getCodigo();

			// Valida la respuesta
			Assertions.assertThat(responsePost)
				.hasFieldOrPropertyWithValue("cedula", request.getCedula())
				.hasFieldOrPropertyWithValue("nombre", request.getNombre())
				.hasFieldOrPropertyWithValue("edad", request.getEdad())
				.hasFieldOrPropertyWithValue("ciudad", request.getCiudad())
			;

			// Consulta todos los empleados
			mvc.perform(get("/empleados"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("[0].codigo").exists())
				.andExpect(jsonPath("[0].cedula").exists())
				.andExpect(jsonPath("[0].nombre").exists())
				.andExpect(jsonPath("[0].edad").isNumber())
				.andExpect(jsonPath("[0].ciudad").exists())
			;

			// Consulta el empleado creado
			mvc.perform(get("/empleados/{codigo}", codigo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value(codigo.toString()))
				.andExpect(jsonPath("$.cedula").value(responsePost.getCedula()))
				.andExpect(jsonPath("$.nombre").value(responsePost.getNombre()))
				.andExpect(jsonPath("$.edad").value(responsePost.getEdad()))
				.andExpect(jsonPath("$.ciudad").value(responsePost.getCiudad()))
			;

			// Actualiza el empleado
			request.setEdad(25);
			request.setCiudad("Pereira");
			final EmpleadoResponse responsePut = objectMapper.readValue(
				mvc.perform(
					put("/empleados/{codigo}", codigo)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
				)
					.andExpect(status().isOk())
					.andDo(print())
					.andReturn()
						.getResponse().getContentAsString()
				, EmpleadoResponse.class
			);

			// Valida la respuesta
			Assertions.assertThat(responsePut.getCodigo()).isInstanceOf(UUID.class).isEqualTo(codigo);
			Assertions.assertThat(responsePut.getCedula()).isInstanceOf(String.class).isEqualTo(request.getCedula());
			Assertions.assertThat(responsePut.getNombre()).isInstanceOf(String.class).isEqualTo(request.getNombre());
			Assertions.assertThat(responsePut.getEdad()).isInstanceOf(Integer.class).isEqualTo(request.getEdad());
			Assertions.assertThat(responsePut.getCiudad()).isInstanceOf(String.class).isEqualTo(request.getCiudad());

			// Consulta el empleado actualizado
			mvc.perform(get("/empleados/{codigo}", codigo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value(codigo.toString()))
				.andExpect(jsonPath("$.cedula").value(responsePut.getCedula()))
				.andExpect(jsonPath("$.nombre").value(responsePut.getNombre()))
				.andExpect(jsonPath("$.edad").value(responsePut.getEdad()))
				.andExpect(jsonPath("$.ciudad").value(responsePut.getCiudad()))
			;

			// Hace una búsqueda
			mvc.perform(get("/empleados/buscar/{query}", String.join("+", request.getCedula(), request.getCiudad())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("[0].codigo").value(codigo.toString()))
			;

		} finally {

			// Elimina el empleado creado
			if(Objects.nonNull(codigo)) {
				mvc.perform(delete("/empleados/{codigo}", codigo))
					.andExpect(status().isOk())
				;
			}

		}
	}

}
