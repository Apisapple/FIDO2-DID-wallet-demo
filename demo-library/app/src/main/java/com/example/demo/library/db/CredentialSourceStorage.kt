package com.example.demo.library.db

import com.example.demo.library.model.publickeycredential.PublicKeyCredentialSource
import java.util.UUID

interface CredentialSourceStorage {
    fun store(credSource: PublicKeyCredentialSource)

    fun load(credId: String): PublicKeyCredentialSource?

    fun loadAll(aaguid: UUID? = null): List<PublicKeyCredentialSource>

    fun delete(credId: String)

    fun getSignatureCounter(credId: String): UInt

    fun increaseSignatureCounter(credId: String)
}
