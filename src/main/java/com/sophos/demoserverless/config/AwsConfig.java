package com.sophos.demoserverless.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

	@Value("${aws.region:us-west-1}")
	private String awsRegion;

	@Bean
	public DynamoDBMapper dynamoDbMapper() {
		final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
			.withRegion(awsRegion)
			.build()
		;
		return new DynamoDBMapper(dynamoDB);
	}

	@Bean
	public AmazonSQS amazonSQS() {
		return AmazonSQSClientBuilder.standard()
			.withRegion(awsRegion)
			.build()
		;
	}

}
