package com.raonsecure.demoserver.data

class PublicKeyCredentialUserEntity(
    var id: ByteArray,
    var displayName: String,
    name: String,
    icon: String,
) : PublicKeyCredentialEntity(
        name = name,
        icon = icon,
    )
