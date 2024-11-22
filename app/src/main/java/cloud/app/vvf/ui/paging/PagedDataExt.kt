package cloud.app.vvf.ui.paging

import cloud.app.vvf.common.helpers.PagedData


fun <T : Any> PagedData<T>.toFlow() = when (this) {
    is PagedData.Single -> SingleSource({ loadItems() }, { clear() }).toFlow()
    is PagedData.Continuous -> ContinuationSource<T, String>({ loadPage(it) }, { invalidate(it) }).toFlow()
}
