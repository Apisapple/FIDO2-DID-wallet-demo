package com.raonsecure.demoserver.base.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "RP")
class RpEntity() {
    @Id
    var id: String = ""

    @Column(nullable = false, length = 128)
    var name: String = ""

    @Column
    var icon: String? = null

    @Column
    var description: String? = null

    constructor(id: String, name: String, icon: String? = null, description: String? = null) : this() {
        require(!(id.isBlank() || name.isBlank())) { "id must not be blank" }

        this.id = id
        this.name = name
        this.icon = icon
        this.description = description
    }
}
