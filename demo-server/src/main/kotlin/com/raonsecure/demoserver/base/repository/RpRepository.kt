package com.raonsecure.demoserver.base.repository

import com.raonsecure.demoserver.base.entity.RpEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RpRepository : JpaRepository<RpEntity, String>
