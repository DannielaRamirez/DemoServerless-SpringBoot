package com.sophos.demoserverless.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;

@Data
@DynamoDBTable(tableName = "DemoServerless")
public class Empleado {

	@DynamoDBHashKey
	private String hk;

	@DynamoDBRangeKey
	private String sk;

	@DynamoDBIndexHashKey(globalSecondaryIndexName = "cedula-index")
	private String cedula;

	@DynamoDBAttribute
	private String nombre;

	@DynamoDBAttribute
	private Integer edad;

	@DynamoDBAttribute
	private String ciudad;

	@DynamoDBAttribute
	private String busqueda;

}
