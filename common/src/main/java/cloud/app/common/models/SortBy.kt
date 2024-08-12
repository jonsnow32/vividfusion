package cloud.app.common.models

enum class SortBy(val serializedName: String) {
  TITLE("title"),
  RELEASED("released"),
  RATING("rating"),
  POPULARITY("popularity"),
  RUNTIME("runtime"),
}
