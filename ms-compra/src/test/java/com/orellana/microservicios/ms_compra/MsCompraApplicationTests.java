package com.orellana.microservicios.ms_compra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// El contexto de prueba usa el perfil test (H2 en memoria, sin Eureka),
// para no depender de credenciales locales de MySQL ni de Eureka.
@SpringBootTest
@ActiveProfiles("test")
class MsCompraApplicationTests {

	@Test
	void contextLoads() {
	}

}
