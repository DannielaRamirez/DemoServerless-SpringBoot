package com.sophos.demoserverless.controller;

import com.sophos.demoserverless.beans.EmpleadoRequest;
import com.sophos.demoserverless.beans.EmpleadoResponse;
import com.sophos.demoserverless.service.EmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/empleados")
@CrossOrigin
public class EmpleadoController {

	private final EmpleadoService empleadoService;

	@Autowired
	public EmpleadoController(EmpleadoService empleadoService) {
		this.empleadoService = empleadoService;
	}

	@GetMapping("")
	public List<EmpleadoResponse> getAll() {
		return empleadoService.getAll();
	}

	@GetMapping("/{codigo}")
	public EmpleadoResponse get(@PathVariable UUID codigo) {
		return empleadoService.get(codigo);
	}

	@PostMapping("")
	public EmpleadoResponse post(@RequestBody EmpleadoRequest request) {
		return empleadoService.post(request);
	}

	@PutMapping("/{codigo}")
	public EmpleadoResponse put(@PathVariable UUID codigo, @RequestBody EmpleadoRequest request) {
		return empleadoService.put(codigo, request);
	}

	@DeleteMapping("/{codigo}")
	public ResponseEntity<HttpStatus> delete(@PathVariable UUID codigo) {
		empleadoService.delete(codigo);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/buscar/{query}")
	public List<EmpleadoResponse> search(@PathVariable String query) {
		return empleadoService.search(query);
	}

}
