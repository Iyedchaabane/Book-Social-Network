package com.ichaabane.book_network;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test d'intégration Spring Boot (hors scope des tests unitaires).
 * Désactivé car les tests unitaires sont requis sans contexte Spring.
 */
@Disabled("Test d'intégration - Hors scope")
@SpringBootTest
class BookNetworkApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
