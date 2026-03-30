package com.example.demo.library.model.fido2

import java.security.Signature

data class Fido2UserAuthResult(
    var signature: Signature?,
)
