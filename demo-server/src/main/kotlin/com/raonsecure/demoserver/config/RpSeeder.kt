package com.raonsecure.demoserver.config

import com.raonsecure.demoserver.base.entity.RpEntity
import com.raonsecure.demoserver.base.repository.RpRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class RpSeeder(
    private val rpRepository: RpRepository,
    private val rpProperties: WebAuthnRpProperties,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (!rpRepository.existsById(rpProperties.id)) {
            rpRepository.save(RpEntity(id = rpProperties.id, name = rpProperties.name))
        }
    }
}
