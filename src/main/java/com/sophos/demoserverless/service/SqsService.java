package com.sophos.demoserverless.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sophos.demoserverless.beans.EmpleadoResponse;
import com.sophos.demoserverless.beans.LogRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SqsService {

	private static final String MESSAGE_GROUP_ID = "5BfpLY9VkP6s5CaHaJJsLFnCAdxZN2FQbnXJpZdf";

	@Value("${aws.sqsurl:}")
	private String sqsQueueUrl;

	private final AmazonSQS sqs;
	private final ObjectMapper objectMapper;

	public SqsService(AmazonSQS sqs) {
		this.sqs = sqs;

		objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
	}

	public void queueLog(EmpleadoResponse empleado, String responsable, String metodo) throws JsonProcessingException {
		final LogRequest logRequest = new LogRequest();
		logRequest.setResponsable(responsable);
		logRequest.setMetodo(metodo);
		logRequest.setCodigo(empleado.getCodigo().toString());
		logRequest.setEntidad(empleado);

		final SendMessageRequest messageRequest = new SendMessageRequest()
			.withQueueUrl(sqsQueueUrl)
			.withMessageGroupId(MESSAGE_GROUP_ID)
			.withMessageDeduplicationId(UUID.randomUUID().toString())
			.withMessageBody(objectMapper.writeValueAsString(logRequest))
		;

		sqs.sendMessage(messageRequest);
	}

}
