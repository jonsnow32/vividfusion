package cloud.app.vvf.datastore.app.helper

import cloud.app.vvf.common.models.User
import cloud.app.vvf.datastore.app.AppDataStore


data class CurrentUser(
  val extensionId: String,
  val id: String?
)

const val USERS_FOLDER = "users"
fun AppDataStore.getCurrentUser(id: String?): User? {
  return getKey<User>("$USERS_FOLDER/${id}", null)
}
fun AppDataStore.getAllUsers(id: String?): List<User>? {
  return getKeys<User>("$USERS_FOLDER/", null)
}
