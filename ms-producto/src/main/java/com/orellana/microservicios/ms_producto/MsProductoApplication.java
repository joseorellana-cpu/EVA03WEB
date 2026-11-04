package com.orellana.microservicios.ms_producto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microservicio de negocio Producto: catalogo e inventario con base de
 * datos propia (bd_producto). Se comunica con ms-proveedor por OpenFeign
 * (cliente por nombre de servicio "MS-PROVEEDOR", resuelto con Eureka).
 */
@SpringBootApplication
@EnableFeignClients
public class MsProductoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsProductoApplication.class, args);
	}

}
