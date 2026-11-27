package no.novari.flyt.example.gateway

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "fint.org-id=fintlabs.no",
        "novari.kafka.topic.org-id=fintlabs-no",
        "spring.kafka.admin.auto-create=false",
        "fint.flyt.authorization.sso.client-id=internal-client",
        "fint.flyt.authorization.sso.client-secret=test",
        "fint.sso.client-id=file-client",
        "fint.sso.client-secret=test",
    ],
)
@AutoConfigureMockMvc
class ApplicationTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Test
    fun `example routes are closed until the external source application is configured`() {
        mvc
            .perform(
                post("/api/example/instances/simple")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isUnauthorized)
    }
}
