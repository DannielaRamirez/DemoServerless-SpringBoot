package com.sophos.demoserverless.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.sophos.demoserverless.beans.EmpleadoRequest;
import com.sophos.demoserverless.beans.EmpleadoResponse;
import com.sophos.demoserverless.model.Empleado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmpleadoService {

	private static final String HK_PARAMETRO = "EMPLEADO";
	private static final Logger LOGGER = LoggerFactory.getLogger(EmpleadoService.class);

	private final DynamoDBMapper mapper;

	@Autowired
	public EmpleadoService(DynamoDBMapper mapper) {
		this.mapper = mapper;
	}

	public List<EmpleadoResponse> getAll() {
		final Map<String, AttributeValue> eav = new HashMap<>();
		eav.put(":hk", new AttributeValue(HK_PARAMETRO));

		final DynamoDBQueryExpression<Empleado> queryExpression = new DynamoDBQueryExpression<Empleado>()
			.withKeyConditionExpression("hk = :hk")
			.withExpressionAttributeValues(eav)
		;

		final List<Empleado> empleados = mapper.query(Empleado.class, queryExpression);
		if(empleados.isEmpty()) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No existe ningún registro"
			);
		}

		return empleados.stream().map(this::mapResponse).collect(Collectors.toList());
	}

	public EmpleadoResponse get(UUID codigo) {
		final Empleado empleado = mapper.load(Empleado.class, HK_PARAMETRO, codigo.toString());
		if(Objects.isNull(empleado)) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No existe el código '" + codigo + "'"
			);
		}
		return mapResponse(empleado);
	}

	public EmpleadoResponse post(EmpleadoRequest request) {
		final Empleado empleado = new Empleado();
		empleado.setHk(HK_PARAMETRO);
		empleado.setSk(UUID.randomUUID().toString());
		mapRequest(empleado, request);

		mapper.save(empleado);

		return mapResponse(empleado);
	}

	public EmpleadoResponse put(UUID codigo, EmpleadoRequest request) {
		get(codigo);

		final Empleado empleado = new Empleado();
		empleado.setHk(HK_PARAMETRO);
		empleado.setSk(codigo.toString());
		mapRequest(empleado, request);

		mapper.save(empleado);

		return mapResponse(empleado);
	}

	public void delete(UUID codigo) {
		final Empleado empleado = new Empleado();
		empleado.setHk(HK_PARAMETRO);
		empleado.setSk(codigo.toString());
		mapper.delete(empleado);
	}

	private void mapRequest(Empleado empleado, EmpleadoRequest request) {
		empleado.setCedula(request.getCedula());
		empleado.setNombre(request.getNombre());
		empleado.setEdad(request.getEdad());
		empleado.setCiudad(request.getCiudad());
		LOGGER.info("Request: {}", request);
	}

	private EmpleadoResponse mapResponse(Empleado empleado) {
		final EmpleadoResponse response = new EmpleadoResponse();
		response.setCodigo(UUID.fromString(empleado.getSk()));
		response.setCedula(empleado.getCedula());
		response.setNombre(empleado.getNombre());
		response.setEdad(empleado.getEdad());
		response.setCiudad(empleado.getCiudad());
		LOGGER.info("Response: {}", response);
		return response;
	}

}
