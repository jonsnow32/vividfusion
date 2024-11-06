package cloud.app.common.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PagedData<T : Any> {
  abstract fun clear()
  abstract suspend fun loadFirst(): List<T>
  abstract suspend fun loadAll(): List<T>

  class Single<T : Any>(
    private val load: suspend () -> List<T>
  ) : PagedData<T>() {
    private var loaded = false
    private val items = mutableListOf<T>()

    suspend fun loadItems(): List<T> {
      if (!loaded) {
        items.addAll(
          withContext(Dispatchers.IO) {
            load()
          }
        )
        loaded = true
      }
      return items
    }

    override fun clear() {
      println("Clearing items")
      items.clear()
      loaded = false
    }

    override suspend fun loadFirst() = loadItems()
    override suspend fun loadAll() = loadItems()
  }

  class Continuous<T : Any>(
    private val load: suspend (String?) -> Page<T, String?>
  ) : PagedData<T>() {
    private val itemMap = mutableMapOf<String?, Page<T, String?>>()

    suspend fun loadPage(continuation: String?): Page<T, String?> {
      return withContext(Dispatchers.IO) {
        itemMap.getOrPut(continuation) {
          load(continuation)
        }
      }
    }

    fun invalidate(continuation: String?) {
      itemMap.remove(continuation)
    }

    override fun clear() = itemMap.clear()

    override suspend fun loadFirst(): List<T> = loadPage(null).data

    override suspend fun loadAll(): List<T> {
      val allItems = mutableListOf<T>()
      var continuation: String? = null

      do {
        val page = loadPage(continuation)
        allItems.addAll(page.data)
        continuation = page.continuation
      } while (continuation != null)

      return allItems
    }
  }
}

