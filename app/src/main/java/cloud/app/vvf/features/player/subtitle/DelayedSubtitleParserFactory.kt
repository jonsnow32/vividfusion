package cloud.app.vvf.features.player.subtitle

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.CuesWithTimingSubtitle
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import com.google.common.collect.ImmutableList
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipInputStream

/**
 * A custom [SubtitleParser.Factory] that applies a delay to subtitle timelines, adjustable at runtime.
 * It wraps an existing factory and adjusts the timing of [CuesWithTiming] instances by a specified delay in milliseconds.
 * Supports all subtitle formats provided by Media3's DefaultSubtitleParserFactory and handles zipped subtitle files.
 * Uses a thread-safe cache to optimize subtitle data processing.
 */
@OptIn(UnstableApi::class)
class DelayedSubtitleParserFactory(
  private val delegateFactory: SubtitleParser.Factory = DefaultSubtitleParserFactory()
) : SubtitleParser.Factory {

  private val delayUs = AtomicLong(0) // Delay in microseconds, thread-safe

  init {
    Assertions.checkNotNull(delegateFactory)
  }

  /**
   * Sets the subtitle delay in milliseconds, safe to call from any thread.
   *
   * @param delayMs The delay to apply to subtitle timings, in milliseconds.
   */
  fun setDelayMs(delayMs: Long) {
    delayUs.set(delayMs * 1_000) // Convert milliseconds to microseconds
  }

  /**
   * Gets the current subtitle delay in milliseconds.
   *
   * @return The current delay in milliseconds.
   */
  fun getDelayMs(): Long {
    return delayUs.get() / 1_000 // Convert microseconds to milliseconds
  }

  override fun supportsFormat(format: Format): Boolean {
    return delegateFactory.supportsFormat(format)
  }

  override fun getCueReplacementBehavior(format: Format): Int {
    return delegateFactory.getCueReplacementBehavior(format)
  }

  override fun create(format: Format): SubtitleParser {
    val delegateParser = delegateFactory.create(format)
    return DelayedSubtitleParser(delegateParser, delayUs)
  }

  /**
   * A [SubtitleParser] that applies a dynamic delay to subtitle timings.
   */
  private class DelayedSubtitleParser(
    private val delegate: SubtitleParser,
    private val delayUs: AtomicLong
  ) : SubtitleParser {

    private val maxCacheSize = 100 // Maximum number of cache entries
    private val subtitleCache = ConcurrentHashMap<String, ByteArray>()

    override fun parse(
      data: ByteArray,
      offset: Int,
      length: Int,
      outputOptions: SubtitleParser.OutputOptions,
      output: Consumer<CuesWithTiming>
    ) {
      val processedData = maybeUnzipSubtitleData(data, offset, length)
      delegate.parse(processedData, 0, processedData.size, outputOptions) { cuesWithTiming ->
        val currentDelayUs = delayUs.get()
        output.accept(
          CuesWithTiming(
            cuesWithTiming.cues,
            cuesWithTiming.startTimeUs + currentDelayUs,
            cuesWithTiming.durationUs
          )
        )
      }
    }

    override fun parseToLegacySubtitle(data: ByteArray, offset: Int, length: Int): Subtitle {
      val actualData = maybeUnzipSubtitleData(data, offset, length)
      val cuesWithTimingList = ImmutableList.builder<CuesWithTiming>()
      delegate.parse(
        actualData,
        0,
        actualData.size,
        SubtitleParser.OutputOptions.allCues(),
        cuesWithTimingList::add
      )
      return CuesWithTimingSubtitle(cuesWithTimingList.build())
    }

    override fun reset() {
      delegate.reset()
    }

    override fun getCueReplacementBehavior(): Int {
      return delegate.cueReplacementBehavior
    }

    private fun maybeUnzipSubtitleData(data: ByteArray, offset: Int, length: Int): ByteArray {
      // Generate a cache key based on the input data
      val cacheKey = data.copyOfRange(offset, offset + length).contentHashCode().toString()

      // Check cache first
      subtitleCache[cacheKey]?.let { return it }

      // Process data (cache miss)
      val header = data.copyOfRange(offset, offset + 4)
      val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
      val processedData = if (!isZip) {
        data.copyOfRange(offset, offset + length)
      } else {
        val inputStream = ByteArrayInputStream(data, offset, length)
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        val supportedExtensions = listOf(
          ".srt", ".vtt", ".ttml", ".dfxp", ".xml", ".ssa", ".ass", ".sub", ".smi"
        )

        var result: ByteArray? = null
        while (entry != null) {
          if (!entry.isDirectory && supportedExtensions.any { ext ->
              entry.name.endsWith(ext, ignoreCase = true)
            }) {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var count: Int
            while (zipInputStream.read(buffer).also { count = it } != -1) {
              output.write(buffer, 0, count)
            }
            result = output.toByteArray()
            break
          }
          entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        result ?: throw IllegalArgumentException("No supported subtitle file found in zip archive")
      }

      // Store in cache with eviction policy
      if (subtitleCache.size >= maxCacheSize) {
        subtitleCache.keys.take(maxCacheSize / 2).forEach { subtitleCache.remove(it) }
      }
      subtitleCache[cacheKey] = processedData

      return processedData
    }
  }
}
