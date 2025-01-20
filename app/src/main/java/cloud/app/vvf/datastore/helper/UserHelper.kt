package cloud.app.vvf.datastore.helper

import cloud.app.vvf.common.models.User
import cloud.app.vvf.datastore.DataStore


data class CurrentUser(
  val clientId: String,
  val id: String?
)

const val USERS_FOLDER = "users"
fun DataStore.getCurrentUser(id: String?): User? {
  return getKey<User>("$USERS_FOLDER/${id}", null)
}
fun DataStore.getAllUsers(id: String?): List<User>? {
  return getKeys<User>("$USERS_FOLDER/", null)
}
