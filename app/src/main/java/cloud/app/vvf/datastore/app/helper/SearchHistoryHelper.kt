package cloud.app.vvf.datastore.app.helper

import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.datastore.app.AppDataStore


const val SEARCH_HISTORY_FOLDER = "search_history"

fun AppDataStore.getSearchHistory(): List<SearchItem>? {
  return getKeys<SearchItem>("$SEARCH_HISTORY_FOLDER/", null)?.sortedByDescending { it.searchedAt }
}

fun AppDataStore.deleteHistorySearch(item: SearchItem){
  return removeKey("$SEARCH_HISTORY_FOLDER/${item.id}")
}

fun AppDataStore.clearHistorySearch(){
  return removeKey("$SEARCH_HISTORY_FOLDER/")
}

fun AppDataStore.saveSearchHistory(item: SearchItem){
  return setKey("$SEARCH_HISTORY_FOLDER/${item.id}", item)
}
