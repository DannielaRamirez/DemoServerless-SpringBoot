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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EmpleadoService {

	private static final String HK_PARAMETRO = "EMPLEADO";
	private static final String GSI_CEDULA = "cedula-index";
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
		validateCedula(request.getCedula(), null);

		final Empleado empleado = new Empleado();
		empleado.setHk(HK_PARAMETRO);
		empleado.setSk(UUID.randomUUID().toString());
		mapRequest(empleado, request);

		mapper.save(empleado);

		return mapResponse(empleado);
	}

	public EmpleadoResponse put(UUID codigo, EmpleadoRequest request) {
		get(codigo);
		validateCedula(request.getCedula(), codigo);

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

	public List<EmpleadoResponse> search(String query) {
		final AtomicInteger idx = new AtomicInteger();
		final Map<String, AttributeValue> eav = new HashMap<>();
		final String filterExpression = Stream.of(query.split("\\s*\\+\\s*"))
			.map(String::toLowerCase)
			.map(filtro -> {
				final String placeholder = ":var" + idx.incrementAndGet();
				eav.put(placeholder, new AttributeValue(filtro));
				return "contains(busqueda, " + placeholder + ")";
			})
			.collect(Collectors.joining(" and "))
		;
		eav.put(":hk", new AttributeValue(HK_PARAMETRO));

		final DynamoDBQueryExpression<Empleado> queryExpression = new DynamoDBQueryExpression<Empleado>()
			.withKeyConditionExpression("hk = :hk")
			.withFilterExpression(filterExpression)
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

	private void mapRequest(Empleado empleado, EmpleadoRequest request) {
		empleado.setCedula(request.getCedula());
		empleado.setNombre(request.getNombre());
		empleado.setEdad(request.getEdad());
		empleado.setCiudad(request.getCiudad());
		empleado.setBusqueda(generateSearchField(empleado));
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
		final Empleado empleado = new Empleado();
		empleado.setCedula(cedula);

		final DynamoDBQueryExpression<Empleado> queryExpression = new DynamoDBQueryExpression<>();
		queryExpression.setHashKeyValues(empleado);
		queryExpression.setIndexName(GSI_CEDULA);
		queryExpression.setConsistentRead(false);

		final List<Empleado> empleados = mapper.query(Empleado.class, queryExpression);
		if(!empleados.isEmpty() && (Objects.isNull(codigo) || !empleados.get(0).getSk().equals(codigo.toString()))) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"Ya existe la cédula '" + cedula + "'"
			);
		}
	}

}
