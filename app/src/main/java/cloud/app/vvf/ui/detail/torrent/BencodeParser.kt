package cloud.app.vvf.ui.detail.torrent

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

data class TorrentInfo(
  val name: String,
  val totalSize: Long,
  val fileCount: Int,
  val infoHash: String,
  val files: List<TorrentFile> = emptyList(),
  val announce: String? = null,
  val announceList: List<List<String>> = emptyList(),
  val creationDate: Long? = null,
  val comment: String? = null,
  val createdBy: String? = null,
  val pieceLength: Long? = null,
  val pieces: ByteArray? = null,
  val isPrivate: Boolean = false,
  val encoding: String? = null
)

data class TorrentFile(
  val path: String,
  val size: Long
)

class BencodeParser {
  private lateinit var input: ByteArrayInputStream
  private var position: Int = 0

  fun parseTorrent(filePath: String): TorrentInfo {
    val bytes = File(filePath).readBytes()
    input = ByteArrayInputStream(bytes)
    val root = parseBencode() as Map<*, *>

    val info = root["info"] as Map<*, *>
    val name = String(info["name"] as ByteArray, StandardCharsets.UTF_8)
    val pieceLength = (info["piece length"] as? Long)
    val pieces = info["pieces"] as? ByteArray
    val isPrivate = (info["private"] as? Long) == 1L
    val encoding = root["encoding"]?.let { String(it as ByteArray, StandardCharsets.UTF_8) }

    // Infohash (SHA-1 of bencoded info)
    val infoBytes = encodeBencode(info)
    val infoHash = sha1(infoBytes).toHexString()

    // Announce and announce-list
    val announce = root["announce"]?.let { String(it as ByteArray, StandardCharsets.UTF_8) }
    val announceList = (root["announce-list"] as? List<*>)?.map { tier ->
      (tier as? List<*>)?.map {
        when (it) {
          is ByteArray -> String(it, StandardCharsets.UTF_8)
          else -> it.toString()
        }
      } ?: emptyList()
    } ?: emptyList()

    // Creation date
    val creationDate = (root["creation date"] as? Long)?.let { it * 1000 }
    // Comment
    val comment = root["comment"]?.let { String(it as ByteArray, StandardCharsets.UTF_8) }
    // Created by
    val createdBy = root["created by"]?.let { String(it as ByteArray, StandardCharsets.UTF_8) }

    // Single-file or multi-file
    val (totalSize, fileCount, files) = if ((info["files"] as? List<*>) != null) {
      val filesList = info["files"] as? List<*>
      val files = filesList?.mapNotNull { file ->
        val fileMap = file as? Map<*, *>
        val pathList = fileMap?.get("path") as? List<*>
        val path = pathList?.joinToString("/") {
          when (it) {
            is ByteArray -> String(it, StandardCharsets.UTF_8)
            else -> it.toString()
          }
        } ?: "unknown"
        val length = (fileMap?.get("length") as? Number)?.toLong() ?: 0L
        if (path.isNotEmpty()) TorrentFile(path, length) else null
      } ?: emptyList()
      val totalSize = files.sumOf { it.size }
      Triple(totalSize, files.size, files)
    } else {
      val length = (info["length"] as? Number)?.toLong() ?: 0L
      Triple(length, 1, listOf(TorrentFile(name, length)))
    }

    return TorrentInfo(
      name = name,
      totalSize = totalSize,
      fileCount = fileCount,
      infoHash = infoHash,
      files = files,
      announce = announce,
      announceList = announceList,
      creationDate = creationDate,
      comment = comment,
      createdBy = createdBy,
      pieceLength = pieceLength,
      pieces = pieces,
      isPrivate = isPrivate,
      encoding = encoding
    )
  }

  private fun parseBencode(): Any {
    val char = input.read().toChar()
    return when {
      char.isDigit() -> parseString(char)
      char == 'i' -> parseInteger()
      char == 'l' -> parseList()
      char == 'd' -> parseDictionary()
      else -> throw IllegalStateException("Invalid Bencode format at position $position")
    }
  }

  private fun parseString(firstChar: Char): ByteArray {
    var lenStr = firstChar.toString()
    while (input.available() > 0) {
      val char = input.read().toChar()
      if (char == ':') break
      lenStr += char
    }
    val length = lenStr.toInt()
    val bytes = ByteArray(length)
    input.read(bytes)
    position += length
    return bytes
  }

  private fun parseInteger(): Long {
    var numStr = ""
    while (input.available() > 0) {
      val char = input.read().toChar()
      if (char == 'e') break
      numStr += char
    }
    return numStr.toLong()
  }

  private fun parseList(): List<Any> {
    val list = mutableListOf<Any>()
    while (input.available() > 0) {
      val char = input.peek().toChar()
      if (char == 'e') {
        input.read() // Skip 'e'
        break
      }
      list.add(parseBencode())
    }
    return list
  }

  private fun parseDictionary(): Map<String, Any> {
    val dict = mutableMapOf<String, Any>()
    while (input.available() > 0) {
      val char = input.peek().toChar()
      if (char == 'e') {
        input.read() // Skip 'e'
        break
      }
      val key = String(parseBencode() as ByteArray, StandardCharsets.UTF_8)
      val value = parseBencode()
      dict[key] = value
    }
    return dict
  }

  private fun ByteArrayInputStream.peek(): Int {
    mark(1)
    val byte = read()
    reset()
    return byte
  }

  // Hàm tính SHA-1
  private fun sha1(data: ByteArray): ByteArray {
    return java.security.MessageDigest.getInstance("SHA-1").digest(data)
  }

  private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
  }

  // Hàm mã hóa Bencode để tính infohash (đơn giản hóa)
  private fun encodeBencode(data: Any): ByteArray {
    return when (data) {
      is ByteArray -> "i${data.size}:${String(data, StandardCharsets.UTF_8)}".toByteArray()
      is String -> "i${data.length}:${data}".toByteArray()
      is Long -> "i${data}e".toByteArray()
      is List<*> -> {
        val encoded = data.joinToString("") { String(encodeBencode(it!!)) }
        "l${encoded}e".toByteArray()
      }
      is Map<*, *> -> {
        val encoded = data.entries.sortedBy { it.key as String }.joinToString("") {
          val key = it.key as String
          val value = encodeBencode(it.value!!)
          "${key.length}:${key}${String(value)}"
        }
        "d${encoded}e".toByteArray()
      }
      else -> throw IllegalArgumentException("Unsupported type")
    }
  }
}
