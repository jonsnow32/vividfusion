package cloud.app.vvf.common.models.stream

enum class PremiumType(var value: Int) {
  Free(0),
  Real_Debrid(1),
  All_Debrid(2),
  Premiumize(4),
  JustWatch(8);

  companion object {
    private val VALUES = values()
    fun getByValue(value: Int) = VALUES.firstOrNull { it.value == value }
  }
}
