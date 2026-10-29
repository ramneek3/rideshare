package org.example.rideshare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "jwt.secret=test-secret-that-is-at-least-32-characters")
@Import(TestcontainersConfig.class)
class RideshareApplicationTests {

	@Test
	void contextLoads() {
	}

}
