package com.sophos.demoserverless.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ping")
@CrossOrigin
public class Ping {

	@GetMapping
	public Map<String, String> ping() {
		return Map.of("ping", UUID.randomUUID().toString());
	}

}
