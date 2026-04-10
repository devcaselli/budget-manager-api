package br.com.casellisoftware.budgetmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: boots the full Spring context to verify that all beans wire
 * correctly — no {@code NoSuchBeanDefinitionException} or circular dependency
 * surprises. Uses Testcontainers so Mongo is available during startup.
 */
@SpringBootTest
@Testcontainers
class BudgetManagerApplicationTests {

	@Container
	@ServiceConnection
	static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

	@Test
	void contextLoads() {
		// If we get here, the full application context started successfully.
	}

}
