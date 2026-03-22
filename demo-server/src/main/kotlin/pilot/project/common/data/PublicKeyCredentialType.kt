package pilot.project.common.data

enum class PublicKeyCredentialType(
    val value: String,
) {
    PUBLIC_KEY("public-key"),
    ;

    companion object {
        fun fromValue(value: String): PublicKeyCredentialType = entries.firstOrNull { it.value == value } ?: PUBLIC_KEY
    }
}
