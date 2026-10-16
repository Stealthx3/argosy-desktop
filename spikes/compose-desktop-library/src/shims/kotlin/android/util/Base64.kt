package android.util

/** android.util.Base64 over java.util.Base64, for the flags upstream passes. */
object Base64 {
    const val DEFAULT = 0
    const val NO_WRAP = 2

    fun decode(str: String, flags: Int): ByteArray = java.util.Base64.getMimeDecoder().decode(str)

    fun encodeToString(input: ByteArray, flags: Int): String =
        if (flags and NO_WRAP != 0) java.util.Base64.getEncoder().encodeToString(input)
        else java.util.Base64.getMimeEncoder().encodeToString(input)
}
