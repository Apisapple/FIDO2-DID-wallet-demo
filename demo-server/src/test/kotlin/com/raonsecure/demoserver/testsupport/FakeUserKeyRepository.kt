package com.raonsecure.demoserver.testsupport

import com.raonsecure.demoserver.base.entity.UserKeyEntity
import com.raonsecure.demoserver.base.repository.UserKeyRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Builds a [UserKeyRepository] mock backed by a mutable list, so ceremony tests can register a
 * credential and then read it back the way the services do.
 */
fun fakeUserKeyRepository(store: MutableList<UserKeyEntity> = mutableListOf()): UserKeyRepository {
    val repository: UserKeyRepository = mock()
    whenever(repository.save(any<UserKeyEntity>())).thenAnswer { invocation ->
        val entity = invocation.getArgument<UserKeyEntity>(0)
        store.removeAll { it.credentialId == entity.credentialId }
        store.add(entity)
        entity
    }
    whenever(repository.findByCredentialId(any())).thenAnswer { invocation ->
        val credentialId = invocation.getArgument<String>(0)
        store.firstOrNull { it.credentialId == credentialId }
    }
    whenever(repository.findAllByUsername(any())).thenAnswer { invocation ->
        val username = invocation.getArgument<String>(0)
        store.filter { it.username == username }
    }
    whenever(repository.findAllByUserId(any())).thenAnswer { invocation ->
        val userId = invocation.getArgument<String>(0)
        store.filter { it.userId == userId }
    }
    return repository
}
