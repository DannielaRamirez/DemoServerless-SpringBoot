package com.sophos.demoserverless.beans;

import lombok.Data;

@Data
public class EmpleadoRequest {

	private String cedula;
	private String nombre;
	private Integer edad;
	private String ciudad;

}
