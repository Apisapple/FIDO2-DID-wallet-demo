package com.raonsecure.demoserver

import com.raonsecure.demoserver.testsupport.CeremonyFixture
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

/**
 * Walks a full registration and authentication ceremony over real HTTP, so the success paths are
 * exercised through the controllers, the advices and the DTOs rather than through the services
 * alone. The other ceremony tests drive the services directly and would not notice a regression
 * introduced at the web layer.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // A real servlet environment means this cannot share the other context-loading tests' cached
    // context, so it starts a second embedded Redis. That server binds a fixed port, so it is
    // moved aside here; leaving it on the default collides with the context already holding 6379.
    properties = ["spring.data.redis.port=16380"],
)
class CeremonyOverHttpTest {

    @LocalServerPort
    var port: Int = 0

    private val http: HttpClient = HttpClient.newHttpClient()

    private fun postJson(path: String, body: String): String =
        http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        ).body()

    private fun field(json: String, name: String): String =
        Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
            ?: error("no '$name' in $json")

    /**
     * The user handle, read from inside the `user` object. A bare search for `"id"` would match
     * `rp.id` first, which appears earlier in the options response and is not base64url.
     */
    private fun userHandle(optionsJson: String): String {
        val user = Regex("\"user\"\\s*:\\s*\\{([^}]*)}").find(optionsJson)?.groupValues?.get(1)
            ?: error("no 'user' object in $optionsJson")
        return field(user, "id")
    }

    @Test
    fun `registration then authentication succeeds over http`() {
        // The server under test accepts the origin configured in application.yml, so the fixture
        // must sign client data for that origin rather than the random test port.
        val fixture = CeremonyFixture(
            origin = "http://localhost:8080",
            rpId = "localhost",
        )
        val username = "http-probe-${System.nanoTime()}"

        val regOptions = postJson("/attestation/options", """{"username":"$username"}""")
        assertTrue(regOptions.contains("\"status\":\"ok\""), regOptions)

        val created = fixture.register(
            challenge = field(regOptions, "challenge"),
            username = username,
            userHandle = Base64.getUrlDecoder().decode(userHandle(regOptions)),
        )

        val regResult = postJson(
            "/attestation/result",
            """{"id":"${created.credentialId}","rawId":"${created.credentialId}",
                "type":"public-key","response":{"clientDataJSON":"${created.clientDataJSON}",
                "attestationObject":"${created.attestationObject}"}}""",
        )
        assertTrue(regResult.contains("\"success\":true"), regResult)

        val authOptions = postJson("/assertion/options", """{"username":"$username"}""")
        assertTrue(authOptions.contains("\"status\":\"ok\""), authOptions)

        val asserted = fixture.authenticate(
            challenge = field(authOptions, "challenge"),
            credentialId = created.credentialId,
        )

        val authResult = postJson(
            "/assertion/result",
            """{"id":"${asserted.credentialId}","rawId":"${asserted.credentialId}",
                "type":"public-key","response":{"clientDataJSON":"${asserted.clientDataJSON}",
                "authenticatorData":"${asserted.authenticatorData}",
                "signature":"${asserted.signature}","userHandle":"${asserted.userHandle}"}}""",
        )
        assertTrue(authResult.contains("\"success\":true"), authResult)
    }

    /** The narrowed registration advice must not have changed this endpoint's behaviour. */
    @Test
    fun `assetlinks endpoint still serves`() {
        val response = http.send(
            HttpRequest.newBuilder(
                URI.create("http://localhost:$port/.well-known/assetlinks.json"),
            ).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertTrue(response.statusCode() == 200, "status=${response.statusCode()}")
    }
}
