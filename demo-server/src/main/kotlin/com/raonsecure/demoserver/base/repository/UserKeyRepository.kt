package com.raonsecure.demoserver.base.repository

import com.raonsecure.demoserver.base.entity.UserKeyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserKeyRepository : JpaRepository<UserKeyEntity, Long> {
    fun findByCredentialId(credentialId: String): UserKeyEntity?

    fun findAllByUserId(userId: String): List<UserKeyEntity>

    fun findAllByUsername(username: String): List<UserKeyEntity>
}
