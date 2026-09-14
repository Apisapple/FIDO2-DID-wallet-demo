package com.raonsecure.demoserver.base.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "AUTHENTICATOR_TRANSPORT")
class AuthenticatorTransportEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_key_id", nullable = false)
    private lateinit var userKeyEntity: UserKeyEntity

    @Column(nullable = false)
    private var transport: String = ""

    constructor(transport: String) : this() {
        this.transport = transport
    }

    fun link(userKey: UserKeyEntity) {
        this.userKeyEntity = userKey
    }
}
