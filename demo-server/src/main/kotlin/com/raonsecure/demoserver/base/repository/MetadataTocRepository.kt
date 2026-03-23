package com.raonsecure.demoserver.base.repository

import com.raonsecure.demoserver.base.entity.MetadataTocEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MetadataTocRepository : JpaRepository<MetadataTocEntity, Long> {
    fun findFirstByMetadataSourceOrderByNoDesc(metadataSource: String): MetadataTocEntity?
}
