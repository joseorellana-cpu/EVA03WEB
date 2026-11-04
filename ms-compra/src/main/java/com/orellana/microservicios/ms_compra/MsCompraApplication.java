package com.orellana.microservicios.ms_compra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microservicio de negocio Compra: proceso de compra con base de datos
 * propia (bd_compra). Orquesta ms-proveedor y ms-producto via OpenFeign
 * (clientes por nombre de servicio, resueltos con Eureka).
 */
@SpringBootApplication
@EnableFeignClients
public class MsCompraApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCompraApplication.class, args);
	}

}
