package com.raonsecure.demoserver.base.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.Date

@Entity
@Table(name = "USER_KEY")
class UserKeyEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne
    lateinit var rpEntity: RpEntity

    @Column(nullable = false, length = 128)
    var userId: String = ""

    @Column(nullable = false, length = 64)
    var username: String = ""

    @Column(nullable = false, length = 64)
    var userDisplayName: String = ""

    @Column(length = 128)
    var userIcon: String? = null

    @Column(nullable = false, length = 36)
    var aaguid: String = ""

    /**
     * Base64url of a credential ID, which WebAuthn allows to be up to 1023 bytes -- about 1364
     * base64url characters. A 256-character column overflows for authenticators that pack state
     * into the ID rather than issuing a short handle.
     */
    @Column(nullable = false, length = 1400)
    var credentialId: String = ""

    @Column(columnDefinition = "TEXT", nullable = false)
    var publicKey: String = ""

    @Column(nullable = false)
    var signatureAlgorithm: Int = 0

    @Column
    var signCounter: Long? = null

    @Column
    var attestationType: AttestationType? = null

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, mappedBy = "userKeyEntity")
    var transports: MutableList<AuthenticatorTransportEntity> = mutableListOf()

    @Column
    var rk: Boolean? = null

    @Column
    var credProtect: Int? = null

    @Column
    var registeredTimestamp: Date? = null

    @Column
    var authenticatedTimestamp: Date? = null

    companion object {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private val entity = UserKeyEntity()

        fun id(id: Long?) = apply { entity.id = id }

        fun rpEntity(rpEntity: RpEntity) = apply { entity.rpEntity = rpEntity }

        fun userId(userId: String) = apply { entity.userId = userId }

        fun username(username: String) = apply { entity.username = username }

        fun userDisplayName(userDisplayName: String) = apply { entity.userDisplayName = userDisplayName }

        fun userIcon(userIcon: String?) = apply { entity.userIcon = userIcon }

        fun aaguid(aaguid: String) = apply { entity.aaguid = aaguid }

        fun credentialId(credentialId: String) = apply { entity.credentialId = credentialId }

        fun publicKey(publicKey: String) = apply { entity.publicKey = publicKey }

        fun signatureAlgorithm(signatureAlgorithm: Int) = apply { entity.signatureAlgorithm = signatureAlgorithm }

        fun signCounter(signCounter: Long?) = apply { entity.signCounter = signCounter }

        fun attestationType(attestationType: AttestationType?) = apply { entity.attestationType = attestationType }

        fun transports(transports: MutableList<AuthenticatorTransportEntity>) = apply { entity.transports = transports }

        fun rk(rk: Boolean?) = apply { entity.rk = rk }

        fun credProtect(credProtect: Int?) = apply { entity.credProtect = credProtect }

        fun registeredTimestamp(registeredTimestamp: Date?) =
            apply {
                entity.registeredTimestamp = registeredTimestamp
            }

        fun authenticatedTimestamp(authenticatedTimestamp: Date?) =
            apply {
                entity.authenticatedTimestamp = authenticatedTimestamp
            }

        fun build(): UserKeyEntity = entity
    }

    constructor(
        id: Long?,
        rpEntity: RpEntity,
        userId: String,
        username: String,
        userDisplayName: String,
        userIcon: String?,
        aaguid: String,
        credentialId: String,
        publicKey: String,
        signatureAlgorithm: Int,
        signCounter: Long?,
        attestationType: AttestationType?,
        transports: MutableList<AuthenticatorTransportEntity>,
        rk: Boolean?,
        credProtect: Int?,
        registeredTimestamp: Date?,
        authenticatedTimestamp: Date?,
    ) : this() {
        this.id = id
        this.rpEntity = rpEntity
        this.userId = userId
        this.username = username
        this.userDisplayName = userDisplayName
        this.userIcon = userIcon
        this.aaguid = aaguid
        this.credentialId = credentialId
        this.publicKey = publicKey
        this.signatureAlgorithm = signatureAlgorithm
        this.signCounter = signCounter
        this.attestationType = attestationType
        this.transports = transports
        this.rk = rk
        this.credProtect = credProtect
        this.registeredTimestamp = registeredTimestamp
        this.authenticatedTimestamp = authenticatedTimestamp
    }
}
