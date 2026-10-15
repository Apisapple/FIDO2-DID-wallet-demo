package com.raonsecure.demoserver

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * The relying party identity is not in `application.yml` -- an absent identity is rejected at
 * startup by design -- so the profile carrying the demo identity is activated here, exactly as a
 * developer would activate it when running the server locally.
 */
@SpringBootTest
@ActiveProfiles("rp-defaults")
class DemoServerApplicationTests {

    @Test
    fun contextLoads() {
    }

}
