package com.raonsecure.demoserver.base.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "METADATA_TOC")
class MetadataTocEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long = 0L

    @Column(nullable = false, columnDefinition = "TEXT")
    private var metadataSource: String = ""

    @Column(nullable = false)
    private var no: Long = 0L

    @Column(columnDefinition = "TEXT")
    private var legalHeader: String? = null

    @Column(nullable = false)
    private var nextUpdate: String = ""

    @Column(nullable = false, columnDefinition = "TEXT")
    private var encodeMetadataTocPayload: String = ""
}
