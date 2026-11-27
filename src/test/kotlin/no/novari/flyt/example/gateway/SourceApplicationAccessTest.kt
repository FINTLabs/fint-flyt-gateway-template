package no.novari.flyt.example.gateway

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

/**
 * With the external routes and a source application configured, as a gateway sets them up. Only the token decoding is
 * mocked, so flyt-security-starter's checks of the client run as in production. The status route needs neither Kafka
 * nor file-service.
 */
@SpringBootTest(
    properties = [
        "fint.org-id=fintlabs.no",
        "novari.kafka.topic.org-id=fintlabs-no",
        "spring.kafka.admin.auto-create=false",
        "fint.flyt.authorization.sso.client-id=internal-client",
        "fint.flyt.authorization.sso.client-secret=test",
        "fint.sso.client-id=file-client",
        "fint.sso.client-secret=test",
        "novari.flyt.security.external.paths=/api/example/**",
        "novari.flyt.security.external.source-application=ACOS",
        "fint.flyt.acos.sso.client-id=$SOURCE_APPLICATION_CLIENT",
    ],
)
@AutoConfigureMockMvc
class SourceApplicationAccessTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @ParameterizedTest
    @CsvSource("$SOURCE_APPLICATION_CLIENT, 200", "internal-client, 401", "another-client, 401")
    fun `only the configured source application's client reaches the routes`(
        client: String,
        expectedStatus: Int,
    ) {
        // NAM sets aud to the client that asked for a client_credentials token.
        given(jwtDecoder.decode(anyString())).willReturn(
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject(client)
                .audience(listOf(client))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build(),
        )

        mvc
            .perform(get("/api/example/instances/simple/123/status").header("Authorization", "Bearer token"))
            .andExpect(status().`is`(expectedStatus))
    }
}

private const val SOURCE_APPLICATION_CLIENT = "acos-client"
