package com.orellana.microservicios.ms_producto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.codec.ErrorDecoder;

/**
 * Configuración de OpenFeign: registra el ErrorDecoder propio (traduce
 * errores del servicio remoto a excepciones de dominio) y el interceptor
 * que propaga el header X-Usuario.
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    @Bean
    public FeignHeaderInterceptor feignHeaderInterceptor() {
        return new FeignHeaderInterceptor();
    }
}
