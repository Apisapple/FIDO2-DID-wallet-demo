package com.raonsecure.demoserver.base.repository

import com.raonsecure.demoserver.base.entity.AuthenticatorTransportEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthenticatorTransportRepository : JpaRepository<AuthenticatorTransportEntity, Long>
