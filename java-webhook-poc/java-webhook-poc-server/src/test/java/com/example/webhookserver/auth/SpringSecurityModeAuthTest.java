package com.example.webhookserver.auth;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the registration endpoint contract under {@code webhook.auth.mode=spring-security}.
 * The same tokens minted by {@link JwtUtils} are validated by Spring's OAuth2
 * resource server, demonstrating format interoperability between the two modes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "webhook.auth.mode=spring-security",
        "webhook.auth.issuer=" + RegistrationAuthTestSupport.ISSUER,
        "webhook.auth.audience=" + RegistrationAuthTestSupport.AUDIENCE,
        "webhook.event.interval=3600000"
})
class SpringSecurityModeAuthTest extends RegistrationAuthTestSupport {
}
