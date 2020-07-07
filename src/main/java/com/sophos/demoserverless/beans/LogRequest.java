package com.sophos.demoserverless.beans;

import lombok.Data;

@Data
public class LogRequest {

	private String responsable;
	private String metodo;
	private String codigo;
	private EmpleadoResponse entidad;

}
