package com.example.demo.library.authenticator

import com.example.demo.library.db.CredentialSourceStorage
import com.example.demo.library.handler.AuthenticationHandler

internal class Authenticator(
    val db: CredentialSourceStorage,
    val authenticationHandler: AuthenticationHandler,
)
