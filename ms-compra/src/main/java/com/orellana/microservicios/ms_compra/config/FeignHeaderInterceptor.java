package com.orellana.microservicios.ms_compra.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Interceptor de Feign: propaga el header "X-Usuario" de la petición
 * entrante hacia ms-proveedor y ms-producto, para que la auditoría de
 * los servicios remotos registre el mismo usuario.
 */
public class FeignHeaderInterceptor implements RequestInterceptor {

    public static final String HEADER_USUARIO = "X-Usuario";

    @Override
    public void apply(RequestTemplate plantilla) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String usuario = atributos.getRequest().getHeader(HEADER_USUARIO);
            if (usuario != null && !usuario.isBlank()) {
                plantilla.header(HEADER_USUARIO, usuario.trim());
            }
        }
    }
}
