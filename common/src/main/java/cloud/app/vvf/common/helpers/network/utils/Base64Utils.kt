package cloud.app.vvf.common.helpers.network.utils

object Base64Util {

  private const val BASE64_CHARS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
  private val BASE64_INV = IntArray(256) { -1 }

  init {
    BASE64_CHARS.forEachIndexed { index, char ->
      BASE64_INV[char.code] = index
    }
    BASE64_INV['='.code] = 0 // Padding character
  }

  fun encode(bytes: ByteArray): String {
    val sb = StringBuilder()
    var paddingCount = 0

    for (i in bytes.indices step 3) {
      var b = ((bytes[i].toInt() and 0xFF) shl 16) and 0xFFFFFF
      if (i + 1 < bytes.size) {
        b = b or ((bytes[i + 1].toInt() and 0xFF) shl 8)
      } else {
        paddingCount++
      }
      if (i + 2 < bytes.size) {
        b = b or (bytes[i + 2].toInt() and 0xFF)
      } else {
        paddingCount++
      }

      sb.append(BASE64_CHARS[(b shr 18) and 0x3F])
      sb.append(BASE64_CHARS[(b shr 12) and 0x3F])
      sb.append(if (paddingCount < 2) BASE64_CHARS[(b shr 6) and 0x3F] else '=')
      sb.append(if (paddingCount < 1) BASE64_CHARS[b and 0x3F] else '=')
    }

    return sb.toString()
  }

  fun decode(base64: String): ByteArray {
    val cleanBase64 = base64.trim()
    val paddingCount = when {
      cleanBase64.endsWith("==") -> 2
      cleanBase64.endsWith("=") -> 1
      else -> 0
    }

    val bytes = ByteArray(cleanBase64.length * 3 / 4)
    var byteIndex = 0

    for (i in cleanBase64.indices step 4) {
      val b = (BASE64_INV[cleanBase64[i].code] shl 18) +
        (BASE64_INV[cleanBase64[i + 1].code] shl 12) +
        (BASE64_INV[cleanBase64[i + 2].code] shl 6) +
        BASE64_INV[cleanBase64[i + 3].code]

      bytes[byteIndex++] = ((b shr 16) and 0xFF).toByte()
      if (cleanBase64[i + 2] != '=') {
        bytes[byteIndex++] = ((b shr 8) and 0xFF).toByte()
      }
      if (cleanBase64[i + 3] != '=') {
        bytes[byteIndex++] = (b and 0xFF).toByte()
      }
    }

    return bytes.copyOf(byteIndex - paddingCount)
  }
}
