package com.example.demo.library.model.publickeycredential

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class PublicKeyCredentialSourceTest {
    private val aaguid: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

    @Test
    fun sameValues_areEqual_andHaveSameHashCode() {
        val first = PublicKeyCredentialSource(
            id = "credential-id",
            rpId = "example.com",
            userHandle = "user-handle",
            aaguid = aaguid,
        )
        val second = PublicKeyCredentialSource(
            id = "credential-id",
            rpId = "example.com",
            userHandle = "user-handle",
            aaguid = aaguid,
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun nullUserHandle_isComparedCorrectly() {
        val withNullUserHandle = PublicKeyCredentialSource(
            id = "credential-id",
            rpId = "example.com",
            userHandle = null,
            aaguid = aaguid,
        )
        val withValueUserHandle = PublicKeyCredentialSource(
            id = "credential-id",
            rpId = "example.com",
            userHandle = "user-handle",
            aaguid = aaguid,
        )

        assertNotEquals(withNullUserHandle, withValueUserHandle)
    }
}


