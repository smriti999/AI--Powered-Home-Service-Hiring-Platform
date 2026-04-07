package np.com.ai_poweredhomeservicehiringplatform.common

import java.security.MessageDigest

fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

fun normalizePhoneNumber(value: String): String {
    return value.filter { it.isDigit() }.take(10)
}

fun normalizeGmailEmail(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return ""
    val prefix = trimmed.substringBefore("@").replace(" ", "")
    if (prefix.isBlank()) return ""
    return "$prefix@gmail.com"
}

