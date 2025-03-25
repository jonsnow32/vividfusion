package cloud.app.vvf.common.models


/**
 * A message to be sent to the app.
 *
 * @see MessageFlowProvider
 * @param message The message to be sent.
 * @param action The action to be performed when the message is clicked.
 */
data class Message(
  val message: String,
  val action: Action? = null
) {
  /**
   * An action to be performed when the message is clicked.
   *
   * @param name The  action button's text.
   * @param handler The handler to be called when the action is clicked.
   */
  data class Action(
    val name: String,
    val handler: (() -> Unit)
  )
}
