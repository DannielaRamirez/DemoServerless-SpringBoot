package com.sophos.demoserverless.beans;

import com.sophos.demoserverless.model.Empleado;

public class LogRequest {

	private String responsable;
	private String metodo;
	private Empleado entidad;

	public String getResponsable() {
		return responsable;
	}

	public void setResponsable(String responsable) {
		this.responsable = responsable;
	}

	public String getMetodo() {
		return metodo;
	}

	public void setMetodo(String metodo) {
		this.metodo = metodo;
	}

	public Empleado getEntidad() {
		return entidad;
	}

	public void setEntidad(Empleado entidad) {
		this.entidad = entidad;
	}

}
