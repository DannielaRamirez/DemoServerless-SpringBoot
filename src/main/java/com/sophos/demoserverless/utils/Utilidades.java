package com.sophos.demoserverless.utils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utilidades {

	private Utilidades() {

	}

	public static String capitalize(String texto) {
		return Stream.ofNullable(texto.strip().split("\\s+"))
			.flatMap(Stream::of)
			.map(word -> word.length() == 1 ? word.toUpperCase() : word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
			.collect(Collectors.joining(" "))
		;
	}

}
