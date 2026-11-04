package com.orellana.microservicios.ms_proveedor.config;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resuelve el usuario actual para la auditoría:
 * 1) header HTTP "X-Usuario" si la petición lo trae;
 * 2) "sistema" en cualquier otro caso (procesos automáticos).
 */
public class UsuarioAuditoriaAware implements AuditorAware<String> {

    public static final String HEADER_USUARIO = "X-Usuario";
    public static final String USUARIO_DEFECTO = "sistema";

    @Override
    public Optional<String> getCurrentAuditor() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String usuario = atributos.getRequest().getHeader(HEADER_USUARIO);
            if (usuario != null && !usuario.isBlank()) {
                return Optional.of(usuario.trim());
            }
        }
        return Optional.of(USUARIO_DEFECTO);
    }
}
