package cloud.app.vvf.features.player.utils

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.CuesWithTimingSubtitle
import androidx.media3.extractor.text.Subtitle
import androidx.media3.extractor.text.SubtitleParser
import com.google.common.collect.ImmutableList
import java.util.concurrent.atomic.AtomicLong

/**
 * A custom [SubtitleParser.Factory] that applies a delay to subtitle timelines, adjustable at runtime.
 * It wraps an existing factory and adjusts the timing of [CuesWithTiming] instances by a specified delay in seconds.
 */
@OptIn(UnstableApi::class)
class DelayedSubtitleParserFactory
  (
  private val delegateFactory: SubtitleParser.Factory
) : SubtitleParser.Factory {

  private val delayUs = AtomicLong(0) // Delay in microseconds, thread-safe

  init {
    Assertions.checkNotNull(delegateFactory)
  }

  /**
   * Sets the subtitle delay in seconds, safe to call from any thread.
   *
   * @param delaySeconds The delay to apply to subtitle timings, in seconds.
   */
  fun setDelaySeconds(delaySeconds: Long) {
    delayUs.set(delaySeconds * 1_000_000) // Convert seconds to microseconds
  }

  /**
   * Gets the current subtitle delay in seconds.
   *
   * @return The current delay in seconds.
   */
  fun getDelaySeconds(): Long {
    return delayUs.get() / 1_000_000 // Convert microseconds to seconds
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

    override fun parse(
      data: ByteArray,
      offset: Int,
      length: Int,
      outputOptions: SubtitleParser.OutputOptions,
      output: Consumer<CuesWithTiming>
    ) {
      delegate.parse(data, offset, length, outputOptions) { cuesWithTiming ->
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
      val cuesWithTimingList = ImmutableList.builder<CuesWithTiming>()
      parse(data, offset, length, SubtitleParser.OutputOptions.allCues(), cuesWithTimingList::add)
      return CuesWithTimingSubtitle(cuesWithTimingList.build())
    }

    override fun reset() {
      delegate.reset()
    }

    override fun getCueReplacementBehavior(): Int {
      return delegate.cueReplacementBehavior
    }
  }
}
