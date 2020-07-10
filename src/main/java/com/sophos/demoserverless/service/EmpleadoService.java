package com.sophos.demoserverless.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sophos.demoserverless.beans.EmpleadoRequest;
import com.sophos.demoserverless.beans.EmpleadoResponse;
import com.sophos.demoserverless.beans.LogRequest;
import com.sophos.demoserverless.model.Empleado;
import com.sophos.demoserverless.repository.EmpleadoRepository;
import com.sophos.demoserverless.utils.Utilidades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmpleadoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmpleadoService.class);
	private static final String RESPONSABLE = "SpringBoot";

	private final EmpleadoRepository empleadoRepository;

	private final EmitterProcessor<LogRequest> processor = EmitterProcessor.create();
	private final FluxSink<LogRequest> sink = processor.sink();

	@Autowired
	public EmpleadoService(EmpleadoRepository empleadoRepository, SqsService sqsService) {
		this.empleadoRepository = empleadoRepository;

		processor.subscribe(logRequest -> {
			try {
				sqsService.queueLog(logRequest);
			} catch (JsonProcessingException e) {
				LOGGER.error("Error encolando el log de operaciones", e);
			}
		});
	}

	public List<EmpleadoResponse> getAll() {
		final List<Empleado> empleados = empleadoRepository.findAll();
		if(empleados.isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No existe ningún registro"
			);
		}

		return empleados.stream().map(this::mapResponse).collect(Collectors.toList());
	}

	public EmpleadoResponse get(UUID codigo) {
		final Empleado empleado = empleadoRepository.findById(codigo)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el código '" + codigo + "'"));

		return mapResponse(empleado);
	}

	public EmpleadoResponse post(EmpleadoRequest request) {
		final Empleado empleado = new Empleado();
		empleado.setHk(EmpleadoRepository.HK_EMPLEADOS);
		empleado.setSk(UUID.randomUUID().toString());
		mapAndValidateRequest(empleado, request);

		validateCedula(request.getCedula(), null);

		final EmpleadoResponse response = mapResponse(empleadoRepository.save(empleado));

		sendLogRequest("POST", response);

		return response;
	}

	public EmpleadoResponse put(UUID codigo, EmpleadoRequest request) {
		final Empleado empleado = new Empleado();
		empleado.setHk(EmpleadoRepository.HK_EMPLEADOS);
		empleado.setSk(codigo.toString());
		mapAndValidateRequest(empleado, request);

		get(codigo);
		validateCedula(request.getCedula(), codigo);

		final EmpleadoResponse response = mapResponse(empleadoRepository.save(empleado));

		sendLogRequest("PUT", response);

		return response;
	}

	public void delete(UUID codigo) {
		final List<Empleado> logsEmpleado = empleadoRepository.findLogsByCodigo(codigo);
		empleadoRepository.deleteAllLogs(logsEmpleado);

		empleadoRepository.deleteById(codigo);
	}

	public List<EmpleadoResponse> search(String query) {
		final List<Empleado> empleados = empleadoRepository.search(query);
		if(empleados.isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No existe ningún registro"
			);
		}

		return empleados.stream().map(this::mapResponse).collect(Collectors.toList());
	}

	private void validarRequest(EmpleadoRequest request) {
		final String KEY_VALOR = "valor";
		final String KEY_LONGITUD = "longitud";

		Map.of(
			"cédula", Map.of(KEY_VALOR, request.getCedula(), KEY_LONGITUD, "15"),
			"nombre", Map.of(KEY_VALOR, request.getNombre(), KEY_LONGITUD, "50"),
			"ciudad", Map.of(KEY_VALOR, request.getCiudad(), KEY_LONGITUD, "25")
		).forEach((campo, detalles) -> {
			if(!Utilidades.validarLongitudONulo(detalles.get(KEY_VALOR), Integer.parseInt(detalles.get(KEY_LONGITUD)))) {
				throw new ResponseStatusException(
					HttpStatus.UNPROCESSABLE_ENTITY,
					"Campo '" + campo + "' demasiado largo o nulo (máximo " + detalles.get("longitud") + " caracteres)"
				);
			}
		});
	}

	private void mapAndValidateRequest(Empleado empleado, EmpleadoRequest request) {
		validarRequest(request);

		empleado.setCedula(request.getCedula().strip());
		empleado.setNombre(Utilidades.capitalize(request.getNombre()));
		empleado.setEdad(request.getEdad());
		empleado.setCiudad(Utilidades.capitalize(request.getCiudad()));
		empleado.setBusqueda(generateSearchField(empleado));
	}

	private EmpleadoResponse mapResponse(Empleado empleado) {
		final EmpleadoResponse response = new EmpleadoResponse();
		response.setCodigo(UUID.fromString(empleado.getSk()));
		response.setCedula(empleado.getCedula());
		response.setNombre(empleado.getNombre());
		response.setEdad(empleado.getEdad());
		response.setCiudad(empleado.getCiudad());
		return response;
	}

	private String generateSearchField(Empleado empleado) {
		return String.join(
				" ",
				List.of(
					empleado.getSk(),
					empleado.getCedula(),
					empleado.getNombre(),
					String.valueOf(empleado.getEdad()),
					empleado.getCiudad()
				)
			)
			.strip()
			.toLowerCase()
		;
	}

	private void validateCedula(String cedula, UUID codigo) {
		final List<Empleado> empleados = empleadoRepository.findByCedula(cedula);
		if(!empleados.isEmpty() && (Objects.isNull(codigo) || !empleados.get(0).getSk().equals(codigo.toString()))) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"Ya existe la cédula '" + cedula + "'"
			);
		}
	}

	private void sendLogRequest(String metodo, EmpleadoResponse response) {
		final LogRequest logRequest = new LogRequest();
		logRequest.setResponsable(RESPONSABLE);
		logRequest.setMetodo(metodo);
		logRequest.setCodigo(response.getCodigo().toString());
		logRequest.setEntidad(response);
		sink.next(logRequest);
	}

}
