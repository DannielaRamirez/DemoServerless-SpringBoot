package com.sophos.demoserverless.beans;

import lombok.Data;

import java.util.UUID;

@Data
public class EmpleadoResponse {

	private UUID codigo;
	private String cedula;
	private String nombre;
	private Integer edad;
	private String ciudad;

}
