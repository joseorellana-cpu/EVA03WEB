package com.orellana.microservicios.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de registro y descubrimiento (Eureka): mantiene el nombre y
 * la ubicacion de cada microservicio para que el API Gateway y Feign
 * los resuelvan por nombre de servicio y no por direccion fija.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
	}

}
