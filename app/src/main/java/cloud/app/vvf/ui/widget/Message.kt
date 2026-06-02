package cloud.app.vvf.ui.widget

data class Message(
  val message: String,
  val action: Action? = null
) {
  data class Action(val name: String, val handler: () -> Unit)
}
