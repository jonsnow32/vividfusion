package cloud.app.vvf.ui.main.files

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cloud.app.vvf.common.models.AVPMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

enum class MediaFilter { ALL, VIDEO, AUDIO }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class FilesViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
) : ViewModel() {

  private val _filter = MutableStateFlow(MediaFilter.ALL)
  val filter: StateFlow<MediaFilter> = _filter.asStateFlow()

  private val _query = MutableStateFlow("")

  fun setFilter(f: MediaFilter) { _filter.value = f }
  fun setQuery(q: String) { _query.value = q }

  val files: Flow<PagingData<AVPMediaItem>> =
    combine(_filter, _query.debounce(300)) { f, q -> f to q }
      .flatMapLatest { (f, q) ->
        Pager(
          config = PagingConfig(pageSize = 30, enablePlaceholders = false),
          pagingSourceFactory = { FilesPagingSource(context, f, q) }
        ).flow
      }
      .cachedIn(viewModelScope)
}
