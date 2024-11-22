package cloud.app.vvf.common.helpers.network.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES256Cryptor {

  fun encrypt(plaintext: String, passphrase: String): String? {
    try {
      val keySize = 256
      val ivSize = 128

      // Create empty key and iv
      val key = ByteArray(keySize / 8)
      val iv = ByteArray(ivSize / 8)

      // Create random salt
      val saltBytes = generateSalt(8)

      // Derive key and iv from passphrase and salt
      EvpKDF(passphrase.toByteArray(Charsets.UTF_8), keySize, ivSize, saltBytes, key, iv)

      // Actual encryption
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
      val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

      // Create CryptoJS-like encrypted string
      val sBytes = "Salted__".toByteArray(Charsets.UTF_8)
      val b = ByteArray(sBytes.size + saltBytes.size + cipherBytes.size)
      System.arraycopy(sBytes, 0, b, 0, sBytes.size)
      System.arraycopy(saltBytes, 0, b, sBytes.size, saltBytes.size)
      System.arraycopy(cipherBytes, 0, b, sBytes.size + saltBytes.size, cipherBytes.size)

      // Encode using java.util.Base64
      val base64b = Base64.getEncoder().encodeToString(b)
      return base64b
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }

  fun decrypt(ciphertext: String, passphrase: String): String {
    try {
      val keySize = 256
      val ivSize = 128

      // Decode Base64
      val ctBytes = Base64.getDecoder().decode(ciphertext.toByteArray(Charsets.UTF_8))

      // Extract salt and ciphertext
      val saltBytes = Arrays.copyOfRange(ctBytes, 8, 16)
      val ciphertextBytes = Arrays.copyOfRange(ctBytes, 16, ctBytes.size)

      // Derive key and iv
      val key = ByteArray(keySize / 8)
      val iv = ByteArray(ivSize / 8)
      EvpKDF(passphrase.toByteArray(Charsets.UTF_8), keySize, ivSize, saltBytes, key, iv)

      // Decrypt
      val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
      cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
      val recoveredPlaintextBytes = cipher.doFinal(ciphertextBytes)
      return String(recoveredPlaintextBytes)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return ""
  }

  private fun generateSalt(length: Int): ByteArray {
    val random = SecureRandom()
    return ByteArray(length).apply { random.nextBytes(this) }
  }

  @Throws(NoSuchAlgorithmException::class)
  private fun EvpKDF(
    password: ByteArray,
    keySize: Int,
    ivSize: Int,
    salt: ByteArray,
    resultKey: ByteArray,
    resultIv: ByteArray
  ) {
    EvpKDF(password, keySize, ivSize, salt, 1, "MD5", resultKey, resultIv)
  }

  @Throws(NoSuchAlgorithmException::class)
  private fun EvpKDF(
    password: ByteArray,
    keySize: Int,
    ivSize: Int,
    salt: ByteArray,
    iterations: Int,
    hashAlgorithm: String,
    resultKey: ByteArray,
    resultIv: ByteArray
  ) {
    val keySizeWords = keySize / 32
    val ivSizeWords = ivSize / 32
    val totalSize = keySizeWords + ivSizeWords
    val derivedBytes = ByteArray(totalSize * 4)
    val hasher = MessageDigest.getInstance(hashAlgorithm)
    var block: ByteArray? = null
    var derivedWords = 0

    while (derivedWords < totalSize) {
      if (block != null) hasher.update(block)
      hasher.update(password)
      block = hasher.digest(salt)
      for (i in 1 until iterations) {
        block = hasher.digest(block)
      }
      System.arraycopy(block, 0, derivedBytes, derivedWords * 4,
        Math.min(block.size, (totalSize - derivedWords) * 4))
      derivedWords += block.size / 4
    }

    System.arraycopy(derivedBytes, 0, resultKey, 0, keySizeWords * 4)
    System.arraycopy(derivedBytes, keySizeWords * 4, resultIv, 0, ivSizeWords * 4)
  }
}
