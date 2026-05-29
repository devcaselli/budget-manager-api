package br.com.casellisoftware.budgetmanager;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Autowired
	private SaveWalletBoundary saveWalletBoundary;

	@Autowired
	private SaveSubscriptionBoundary saveSubscriptionBoundary;

	@Autowired
	private PatchSubscriptionBoundary patchSubscriptionBoundary;

	@Autowired
	private DeleteSubscriptionBoundary deleteSubscriptionBoundary;

	@Test
	void contextLoads() {
		// If we get here, the full application context started successfully.
	}

	@Test
	void subscriptionTask6BeansAreWired() {
		assertThat(List.of(
				saveWalletBoundary,
				saveSubscriptionBoundary,
				patchSubscriptionBoundary,
				deleteSubscriptionBoundary
		)).allSatisfy(boundary -> assertThat(AopUtils.isAopProxy(boundary)).isTrue());
	}

}
