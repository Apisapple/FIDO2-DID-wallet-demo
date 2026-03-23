package com.raonsecure.demoserver.base.repository

import com.raonsecure.demoserver.base.entity.MetadataEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MetadataRepository : JpaRepository<MetadataEntity, Long> {
    fun findByAaguid(aaguid: String): MetadataEntity?

    fun findAllByAaguidIsNull(): List<MetadataEntity>
}
