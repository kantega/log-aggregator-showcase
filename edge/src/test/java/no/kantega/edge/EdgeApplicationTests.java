package no.kantega.edge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Disabled by default: requires MongoDB and RabbitMQ running
@EnabledIfEnvironmentVariable(named = "SPRING_INTEGRATION_TEST", matches = "true")
class EdgeApplicationTests {

	@Test
	void contextLoads() {
	}

}
