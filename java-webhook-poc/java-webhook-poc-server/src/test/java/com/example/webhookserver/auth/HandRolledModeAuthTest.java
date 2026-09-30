package com.example.webhookserver.auth;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the registration endpoint contract under {@code webhook.auth.mode=hand-rolled}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "webhook.auth.mode=hand-rolled",
        "webhook.auth.issuer=" + RegistrationAuthTestSupport.ISSUER,
        "webhook.auth.audience=" + RegistrationAuthTestSupport.AUDIENCE,
        // keep the scheduled dispatcher quiet during the test
        "webhook.event.interval=3600000"
})
class HandRolledModeAuthTest extends RegistrationAuthTestSupport {
}
