package com.duhao.security.checkinapp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for Spring context loading.
 * Disabled because it requires Redis which is excluded in test profile.
 * Run manually with Redis available when needed.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires Redis - run manually with docker compose up redis")
class SecuityCheckinApplicationTests {

    @Test
    void contextLoads() {
    }

}
