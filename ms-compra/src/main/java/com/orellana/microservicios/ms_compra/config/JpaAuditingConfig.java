package com.orellana.microservicios.ms_compra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa la auditoría de Spring Data JPA apuntando al bean que
 * resuelve el usuario actual (header X-Usuario o "sistema").
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "usuarioAuditoriaAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> usuarioAuditoriaAware() {
        return new UsuarioAuditoriaAware();
    }
}
