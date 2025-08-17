package cloud.app.vvf.datastore.account

import android.content.Context
import cloud.app.vvf.R
import cloud.app.vvf.common.models.extension.ExtensionMetadata
import cloud.app.vvf.common.models.extension.ExtensionType
import cloud.app.vvf.datastore.DataStore
import kotlinx.serialization.Serializable


const val ACCOUNTS_FOLDER = "accounts"

@Serializable
data class Account(
  val id : Long,
  val name: String,
  val avatar: String, // Lưu tên resource thay vì resId
  val lockPin: String? = null,
  var isActive: Boolean = false,
) {
  fun getSlug() = id
}

class AccountDataStore(val context: Context) :
  DataStore(context.getSharedPreferences("accounts_preference", Context.MODE_PRIVATE)) {
  fun removeAccount(slug: Long) {
    return removeKey("$ACCOUNTS_FOLDER/${slug}")
  }

  fun saveAccount(account: Account) {
    return set("$ACCOUNTS_FOLDER/${account.getSlug()}", account)
  }


  fun setActiveAccount(account: Account): Boolean {
    val oldAccount = getActiveAccount()
    if (oldAccount.getSlug() == account.getSlug())
      return false

    oldAccount.isActive = false;
    saveAccount(oldAccount)

    account.isActive = true;
    saveAccount(account)
    return true
  }

  fun getActiveAccount(): Account {
    return getAll<Account>(
      "$ACCOUNTS_FOLDER/",
    )?.firstOrNull() { account -> account.isActive }
      ?: createDefaultAccount()
  }

  fun getAllAccounts(): List<Account>? {
    return getAll<Account>("$ACCOUNTS_FOLDER/")
  }

  private fun createDefaultAccount(): Account {
    val defaultAccount = Account(
      id = 0,
      name = "Default",
      avatar = "funemoji_2",
      lockPin = null,
      isActive = true
    )

    return get<Account>("$ACCOUNTS_FOLDER/${defaultAccount.getSlug()}")
      ?: defaultAccount.also {
        saveAccount(it)
      }
  }

  fun setVotedExtension(metadata: ExtensionMetadata, type: ExtensionType) {
    val key = "${type.name}/${metadata.className}"
    set(key, true)
  }

  fun checkVoted(metadata: ExtensionMetadata, type: ExtensionType): Boolean {
    val key = "${type.name}/${metadata.className}"
    return get<Boolean>(key) == true
  }
}
