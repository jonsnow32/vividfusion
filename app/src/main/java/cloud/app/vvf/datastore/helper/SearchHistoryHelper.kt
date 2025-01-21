package cloud.app.vvf.datastore.helper

import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.datastore.DataStore
import timber.log.Timber


const val SEARCH_HISTORY_FOLDER = "search_history"

fun DataStore.getSearchHistory(): List<SearchItem>? {
  return getKeys<SearchItem>("$SEARCH_HISTORY_FOLDER/", null)?.sortedByDescending { it.searchedAt }
}

fun DataStore.deleteHistorySearch(item: SearchItem){
  return removeKey("$SEARCH_HISTORY_FOLDER/${item.id}")
}

fun DataStore.clearHistorySearch(){
  return removeKey("$SEARCH_HISTORY_FOLDER/")
}

fun DataStore.saveSearchHistory(item: SearchItem){
  Timber.i("saveSearchHistory ${item.id} ${item.searchedAt}")
  return setKey("$SEARCH_HISTORY_FOLDER/${item.id}", item)
}
