package org.springframework.samples.petclinic.customers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CustomersServiceApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
        // with the test profile (no external dependencies)
    }
}