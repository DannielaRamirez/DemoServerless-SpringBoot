package com.sophos.demoserverless;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {

	private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

	static {
		try {
			handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(DemoServerlessApplication.class);
		} catch (ContainerInitializationException e) {
			throw new IllegalStateException("Could not initialize Spring Boot application", e);
		}
	}

	@Override
	public AwsProxyResponse handleRequest(AwsProxyRequest request, Context context) {
		// Si la ejecución viene del ALB, elimina el path
		context.getLogger().log("Método: " + request.getHttpMethod() + " - Path: " + request.getPath());
		request.setPath(request.getPath().replace("/lambda", ""));

		AwsProxyResponse response = handler.proxy(request, context);

		// Adiciona el content-type como header normal para que el ALB entienda la respuesta
		response.setHeaders(Map.of("Content-Type", "application/json"));
		context.getLogger().log("Body: " + response.getBody());

		return response;
	}

}
