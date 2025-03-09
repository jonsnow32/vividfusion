package cloud.app.vvf.datastore.app

import android.content.Context
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.account.Account

class AppDataStore(val context: Context, val account: Account) : DataStore(context.getSharedPreferences(account.getSlug(), Context.MODE_PRIVATE))
