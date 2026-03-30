package com.example.demo.library.model.publickeycredential

import com.example.demo.library.model.attestation.COSEAlgorithmIdentifier

data class PublicKeyCredentialParams(
    val type: PublicKeyCredentialType,
    val alg: COSEAlgorithmIdentifier,
)
