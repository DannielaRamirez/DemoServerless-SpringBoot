package com.sophos.demoserverless.service;

import com.sophos.demoserverless.beans.EmpleadoRequest;
import com.sophos.demoserverless.beans.EmpleadoResponse;
import com.sophos.demoserverless.model.Empleado;
import com.sophos.demoserverless.repository.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmpleadoService {

	private final EmpleadoRepository empleadoRepository;
	private final SqsService sqsService;

	@Autowired
	public EmpleadoService(EmpleadoRepository empleadoRepository, SqsService sqsService) {
		this.empleadoRepository = empleadoRepository;
		this.sqsService = sqsService;
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
		validateCedula(request.getCedula(), null);

		final Empleado empleado = new Empleado();
		empleado.setHk(EmpleadoRepository.HK_PARAMETRO);
		empleado.setSk(UUID.randomUUID().toString());
		mapRequest(empleado, request);

		final EmpleadoResponse response = mapResponse(empleadoRepository.save(empleado));

		sqsService.queueLog(response, "", "POST");

		return response;
	}

	public EmpleadoResponse put(UUID codigo, EmpleadoRequest request) {
		get(codigo);
		validateCedula(request.getCedula(), codigo);

		final Empleado empleado = new Empleado();
		empleado.setHk(EmpleadoRepository.HK_PARAMETRO);
		empleado.setSk(codigo.toString());
		mapRequest(empleado, request);

		final EmpleadoResponse response = mapResponse(empleadoRepository.save(empleado));

		sqsService.queueLog(response, "", "POST");

		return response;
	}

	public void delete(UUID codigo) {
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

	private void mapRequest(Empleado empleado, EmpleadoRequest request) {
		empleado.setCedula(request.getCedula());
		empleado.setNombre(request.getNombre());
		empleado.setEdad(request.getEdad());
		empleado.setCiudad(request.getCiudad());
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

}
