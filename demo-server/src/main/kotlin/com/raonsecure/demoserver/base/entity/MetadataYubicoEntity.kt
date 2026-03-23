package com.raonsecure.demoserver.base.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "METADATA_YUBICO")
class MetadataYubicoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id: Long? = null

    @Column(columnDefinition = "TEXT", nullable = false)
    private var content: String? = null
}
