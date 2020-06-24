package com.sophos.demoserverless;

import com.sophos.demoserverless.controller.EmpleadoController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({EmpleadoController.class})
public class DemoServerlessApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoServerlessApplication.class, args);
	}

}
