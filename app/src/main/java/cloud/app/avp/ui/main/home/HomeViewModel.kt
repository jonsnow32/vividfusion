package cloud.app.avp.ui.main.home

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cloud.app.avp.ui.main.FeedViewModel
import cloud.app.avp.ui.paging.toFlow
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.infos.FeedClient
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.Tab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
  throwableFlow: MutableSharedFlow<Throwable>,
  override val extensionFlow: MutableStateFlow<BaseExtension?>,
) : FeedViewModel(throwableFlow, extensionFlow) {

  init {
    initialize();
  }

  override suspend fun getTabs(client: BaseExtension): List<Tab>? = (client as? FeedClient)?.getHomeTabs()
  override fun getFeed(client: BaseExtension) = (client as? FeedClient)?.getHomeFeed(tab)?.toFlow()

}

