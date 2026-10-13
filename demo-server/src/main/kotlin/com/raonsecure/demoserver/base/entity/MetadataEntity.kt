package com.raonsecure.demoserver.base.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "METADATA")
class MetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long = 0

    private var aaguid: String = ""

    @Column(nullable = false, columnDefinition = "TEXT")
    private var content: String = ""

    private var biometricStatusReports: String? = null

    private var timeOfLastStatusChange: String? = null

    private var statusReports: String? = null
}
