package cloud.app.avp.ui.main.home

import cloud.app.avp.ui.main.FeedViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.clients.BaseClient
import cloud.app.common.clients.DatabaseExtension
import cloud.app.common.clients.mvdatabase.DatabaseClient
import cloud.app.common.models.Tab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  override val databaseExtensionFlow: MutableStateFlow<DatabaseExtension?>,
) : FeedViewModel(throwableFlow, databaseExtensionFlow) {

  init {
    initialize();
  }

  override suspend fun getTabs(client: BaseClient): List<Tab>? = (client as? DatabaseClient)?.getHomeTabs()
  override fun getFeed(client: BaseClient) = (client as? DatabaseClient)?.getHomeFeed(tab)?.toFlow()
}

