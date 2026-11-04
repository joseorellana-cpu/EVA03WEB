package com.orellana.microservicios.ms_proveedor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// El contexto de prueba usa el perfil test (H2 en memoria, sin Eureka),
// para no depender de las credenciales locales de MySQL del perfil dev.
@SpringBootTest
@ActiveProfiles("test")
class MsProveedorApplicationTests {

	@Test
	void contextLoads() {
	}

}
